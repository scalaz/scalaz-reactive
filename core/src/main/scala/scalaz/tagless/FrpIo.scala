package scalaz.tagless
import scalaz.Monad
import scalaz.reactive.Time
import scalaz.tagless.types.IO1
import scalaz.zio.{Fiber, IO}

import scala.concurrent.duration.Duration

package object types {

  type IO1[A] = IO[Void, A]
}

object FrpIo extends Frp[IO1] {

  override def merge[A](f1: => IO1[A], f2: IO1[A]): IO1[A] = {
    f1.flatMap { _ => // FIXME : we need to compare the times
      f2.map { a2 => a2
      }
    }
  }

  override def merge[A](a: => Event[IO1, A],
                        b: Event[IO1, A]): IO1[Event[IO1, A]] = {

    case class Outcome(value: Reactive[IO1, A],
                       loser: Fiber[Void, Reactive[IO1, A]])

    val futureReactive: IO1[Reactive[IO1, A]] = a.value
      .raceWith(b.value)(
        (a, f) => IO.now(Outcome(a, f)),
        (a, f) => IO.now(Outcome(a, f))
      )
      .flatMap {
        case Outcome(reactive, loser) =>
          val winTail: Event[IO1, A] = reactive.tail
          val j: IO1[Reactive[IO1, A]] = loser.join
          merge(winTail, Event(j)).map(m => Reactive(reactive.head, m))
      }
    monadIo.pure(Event(futureReactive))

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
    e.value.flatMap { r => sinkR(sink, r)
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
