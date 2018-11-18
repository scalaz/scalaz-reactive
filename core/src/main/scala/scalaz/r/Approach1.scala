package scalaz.r

import scalaz.Scalaz._
import scalaz._
import scalaz.r.types.IO1
import scalaz.reactive.{Time}
import scalaz.zio._

import scala.concurrent.duration.{Duration, _}
import scala.language.postfixOps

case class Future[F[_], A](r: F[A])

case class Reactive[F[_], A](head: A, tail: Event[F, A])

case class Event[F[_], A](value: Future[F, Reactive[F, A]])

case class Behaviour[A]()

object imps {

  implicit def monadReactive[F[_]](implicit frp: Frp[F],
                                   monad: Monad[F]): Monad[Reactive[F, ?]] = {
    type R[A] = Reactive[F, A]

    new Monad[R] {
      override def bind[A, B](fa: R[A])(f: A => R[B]): R[B] = {
        val other: Reactive[F, B] = f(fa.head)
        val otherTail: Future[F, Reactive[F, B]] = other.tail.value
        val thisTail: Future[F, Reactive[F, B]] = Future(
          monad.bind(fa.tail.value.r) { r => f(r.head).tail.value.r
          }
        )
        val merged: F[Reactive[F, B]] = frp.merge(thisTail, otherTail)
        Reactive(other.head, Event(Future(merged)))
      }

      override def point[A](a: => A): R[A] =
        Reactive[F, A](a, frp.never)
    }
  }

}

trait Frp[F[_]] {

  type Sink[A] = A => F[Unit]

  def merge[A](a: => Future[F, A], b: Future[F, A]): F[A]

  def merge[A](a: => Event[F, A], b: Event[F, A]): F[Event[F, A]]

  def delay[A](e: => Event[F, A], interval: Duration): F[Event[F, A]]

  def now: F[Time]

  def pure[A](a: => A): F[A]

  def sinkE[A](sink: Sink[A], event: Event[F, A]): F[Unit]

  def sinkR[A](sink: Sink[A], r: Reactive[F, A]): F[Unit]

  def never[A]: Event[F, A]

}

object Ops {

  implicit class eventOps[F[_], A](e: Event[F, A])(implicit frp: Frp[F]) {
    def delay(interval: Duration): F[Event[F, A]] = frp.delay(e, interval)

    def merge(other: Event[F, A]): F[Event[F, A]] = frp.merge(e, other)
  }

}

object TwoTickers1 extends App {

  import Ops._

  case class Tick(name: String)

  class Program[F[_]](implicit val frp: Frp[F], m: Monad[F]) {

    def ticks(interval: Duration, name: String): F[Event[F, Tick]] = {

      def tail(): F[Event[F, Tick]] =
        ticks(interval, name).flatMap(tss => frp.delay(tss, interval))

      val x: F[Reactive[F, Tick]] = for {
        head <- m.point(Tick(name))
        tail <- tail()
      } yield Reactive(head, tail)
      val e: Event[F, Tick] = Event(Future(x))
      m.point(e)
    }

    def myAppLogic: F[Unit] = {
      val sink: Tick => F[Unit] = (t: Tick) =>
        frp.pure(println(s"tick ${t.name}"))

      val eventA: F[Event[F, Tick]] = ticks(1.2 second, "a")
      val eventB: F[Event[F, Tick]] = ticks(2.1 second, "b")
      val merged: F[Event[F, Tick]] =
        m.bind(eventA)(a => m.bind(eventB)(b => a.merge(b)))

      merged.flatMap(frp.sinkE(sink, _))
    }
  }

  implicit val frp = FrpIo
  implicit val m: Monad[IO1] = monadIo

  override def run(args: List[String]): IO[Nothing, TwoTickers1.ExitStatus] = {
    val logic: IO1[Unit] = new Program().myAppLogic
    val attempted: IO[Nothing, Either[Void, Unit]] = logic.attempt
    attempted.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))
  }
}

package object types {

  type IO1[A] = IO[Void, A]
}

object FrpIo extends Frp[IO1] {

  override def merge[A](a: => Future[IO1, A], b: Future[IO1, A]): IO1[A] = {
    println("merge future")
    ???
  }

  override def merge[A](a: => Event[IO1, A],
                        b: Event[IO1, A]): IO1[Event[IO1, A]] = {

    case class Outcome(value: Reactive[IO1, A],
                       loser: Fiber[Void, Reactive[IO1, A]])


    val futureReactive: IO1[Reactive[IO1, A]] = a.value.r.raceWith(b.value.r)(
      (a, f) => IO.now(Outcome(a, f)),
      (a, f) => IO.now(Outcome(a, f))
    )
      .flatMap {
        case Outcome(reactive, loser) =>
          val winTail: Event[IO1, A] = reactive.tail
          val j: IO1[Reactive[IO1, A]] = loser.join
          val fj: Future[IO1, Reactive[IO1, A]] = Future(j)
          merge(winTail, Event(fj)).map(m => Reactive(reactive.head, m))
      }
    pure(Event(Future(futureReactive)))

  }

  def delay[A](e: => Event[IO1, A], interval: Duration): IO1[Event[IO1, A]] =
    IO.sync(e).delay(interval)

  override def now: IO1[Time] = {
    println("now")
    ???
  }
  override def pure[A](a: => A): IO1[A] = IO.now(a)

  override def sinkR[A](sink: Sink[A], r: Reactive[IO1, A]): IO1[Unit] =
    sink(r.head).flatMap(_ => sinkE(sink, r.tail))

  override def sinkE[A](sink: Sink[A], e: Event[IO1, A]): IO1[Unit] =
    e.value.r.flatMap { r => sinkR(sink, r)
    }

  override def never[A]: Event[IO1, A] = {
    println("never")
    ???
  }
}

object monadIo extends Monad[IO1] {
  override def point[A](a: => A): IO1[A] = { IO.point(a) }

  override def bind[A, B](fa: IO1[A])(f: A => IO1[B]): IO1[B] = fa.flatMap(f)

  override def map[A, B](fa: IO1[A])(f: A => B): IO1[B] = fa.map(f)

}
