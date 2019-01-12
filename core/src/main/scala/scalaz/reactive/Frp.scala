package scalaz.reactive

import scalaz.{Monad, Monoid}

import scala.concurrent.duration.Duration

trait Sync[F[_]] extends Monad[F] {
  def force[A](io: F[A], a: A): F[A]

  def suspend[A](thunk: => F[A]): F[A]
  def delay[A](thunk: => A): F[A] = suspend(pure(thunk))
  def race[A](thunk1: => F[A], thunk2: => F[A]): F[(Int, A)]
  def halt[A]: F[A]
}

object Sync {
  @inline def apply[F[_]](implicit F: Sync[F]): Sync[F] = F
}

object Frp {}

case class TimedValue[A](t: Improving[Time], value: Unit => A) {
  def get: A = value(())
}

case class Future[F[_]: Sync, A](ftv: F[TimedValue[A]])

object Future {

  val NoValue: Unit => Nothing = _ => ???

  def apply[F[_]: Sync, A](t: Time, v: A): Future[F, A] =
    Future(Monad[F].pure(TimedValue(Improving.exactly(t), _ => v)))

  def force[F[_]: Sync, A](fut: Future[F, A]): Future[F, A] = {
    Future(Sync[F].force(fut.ftv, TimedValue(Improving.afterNow, NoValue)))
  }

  def forceF[F[_]: Sync, A](t: F[A], a: A): F[A] = {
    Sync[F].force(t, a)
  }

}

case class Reactive[F[_], A](head: A, tail: Event[F, A])

case class Event[F[_], A](value: Future[F, Reactive[F, A]])

case class Behaviour[A]()

trait Frp[F[_]] {

  type Sink[A] = A => F[Unit]

  def merge[A](a: => Event[F, A], b: Event[F, A])(
    implicit monoFut: Monoid[Future[F, A]]
  ): F[Event[F, A]]

  def delay[A](e: => Event[F, A], interval: Duration): F[Event[F, A]]

  def now: F[Time]

  def pure[A](a: => A): F[A]

  def sinkE[A](sink: Sink[A], event: Event[F, A]): F[Unit]

  def sinkR[A](sink: Sink[A], r: Reactive[F, A]): F[Unit]

  def never[A]: Event[F, A]

}

object Ops {

  implicit class eventOps[F[_], A](e: Event[F, A])(
    implicit frp: Frp[F],
    monoFut: Monoid[Future[F, A]]
  ) {
    def delay(interval: Duration): F[Event[F, A]] = frp.delay(e, interval)

    def merge(other: Event[F, A]): F[Event[F, A]] = frp.merge(e, other)
  }

}
