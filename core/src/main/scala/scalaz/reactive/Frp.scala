package scalaz.reactive

import scalaz.Monad

import scala.concurrent.duration.Duration

trait Sync[F[_]] extends Monad[F] {
  def suspend[A](thunk: => F[A]): F[A]
  def delay[A](thunk: => A): F[A] = suspend(pure(thunk))
}

object Sync {
  @inline def apply[F[_]](implicit F: Sync[F]): Sync[F] = F
}

object Frp {
  type FTime = Unit => Time
}

case class Future[F[_]: Sync, A](t: F[Time], v: F[A])

object Future {
  def apply[F[_]: Sync, A](t: Time, v: A): Future[F, A] =
    Future(Monad[F].pure(t), Monad[F].pure(v))

  //  def apply[F[_]: Monad, A](ft: Time, v: F[A]): Future[F, A] = Future(Monad[F].pure(t), v)
}

case class Reactive[F[_], A](head: A, tail: Event[F, A])

case class Event[F[_], A](value: Future[F, Reactive[F, A]])

case class Behaviour[A]()

trait Frp[F[_]] {

  type Sink[A] = A => F[Unit]

  def merge[A](a: => Future[F, A], b: Future[F, A]): Future[F, A]

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
