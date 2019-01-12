package scalaz.reactive.io

import scalaz._
import scalaz.reactive.Time._
import scalaz.reactive._
import scalaz.reactive.io.types.IO1
import scalaz.zio.Errors.LostRace
import scalaz.zio.IO

import scala.concurrent.duration._
import scala.language.postfixOps

package object types {

  type IO1[A] = IO[Void, A]
}

object FrpIo extends Frp[IO1] {

  def delay[A](e: => Event[IO1, A], interval: Duration): IO1[Event[IO1, A]] =
    IO.sync(e).delay(interval)

  override def now: IO1[Time] = IO.sync(T(System.currentTimeMillis()))

  override def pure[A](a: => A): IO1[A] = IO.now(a)

  override def sinkR[A](sink: Sink[A], r: Reactive[IO1, A]): IO1[Unit] =
    sink(r.head).flatMap(_ => sinkE(sink, r.tail))

  override def sinkE[A](sink: Sink[A], e: Event[IO1, A]): IO1[Unit] = {
    e.value.ftv.flatMap(r => {
      val xx: Reactive[IO1, A] = r.value(())
      sinkR(sink, xx)
    })
  }

  override def never[A]: Event[IO1, A] = ???
  override def merge[A](a: => Event[IO1, A], b: Event[IO1, A])(
    implicit monoFut: Monoid[Future[IO1, A]]
  ): IO1[Event[IO1, A]] = ???
}

object instances {

  implicit object monadIo extends Monad[IO1] {
    override def point[A](a: => A): IO1[A] = { IO.point(a) }

    override def bind[A, B](fa: IO1[A])(f: A => IO1[B]): IO1[B] = fa.flatMap(f)

    override def map[A, B](fa: IO1[A])(f: A => B): IO1[B] = fa.map(f)

  }

  implicit object syncIo extends Sync[IO1] {
    override def suspend[A](thunk: => IO1[A]): IO1[A] = thunk

    override def bind[A, B](fa: IO1[A])(f: A => IO1[B]): IO1[B] =
      monadIo.bind(fa)(f)
    override def point[A](a: => A): IO1[A] = monadIo.point(a)
    override def race[A](thunk1: => IO1[A],
                         thunk2: => IO1[A]): IO1[(Int, A)] = {
      val r: IO[Void, (Int, A)] = thunk1.raceWith(thunk2)(
        (a, fiber) => fiber.interrupt(LostRace(Right(fiber))).const((0, a)),
        (a, fiber) => fiber.interrupt(LostRace(Left(fiber))).const((1, a))
      )

      r
    }
    override def halt[A]: IO1[A] = IO.never

    override def force[A](io: IO1[A], a: A): IO1[A] =
      io.timeout(a)(identity)(1 milli)
  }

  implicit def monoFut[A] = new Monoid[Future[IO1, A]] {

    val never: TimedValue[A] = TimedValue(Improving.exactly(Time.PosInf), _ => ???)

    override def zero: Future[IO1, A] = {
      val io: IO1[TimedValue[A]] = IO.point(never)
      Future(io)
    }

    override def append(f1: Future[IO1, A],
                        f2: => Future[IO1, A]): Future[IO1, A] = {

      val t1: IO1[Improving[Time]] = f1.ftv.map(_.t)
      val t2: IO1[Improving[Time]] = f2.ftv.map(_.t)
      val minLE: IO1[(Improving[Time], Boolean)] =
        Improving.minLe(t1, t2, Improving.afterNow)
      val futureTimedValue: IO1[TimedValue[A]] = minLE.flatMap {
        case (it, isLe) =>
          if (isLe)
            f1.ftv.map(tv => TimedValue(it, tv.value))
          else
            f2.ftv.map(tv => TimedValue(it, tv.value))
      }
      Future(futureTimedValue)
    }
  }

}
