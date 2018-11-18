package scalaz.tagless
import scalaz.reactive.Time
import scalaz.tagless.Frp.Future

import scala.concurrent.duration.Duration

object Frp {
  type Future[F[_], A] = F[(Time, A)]
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
