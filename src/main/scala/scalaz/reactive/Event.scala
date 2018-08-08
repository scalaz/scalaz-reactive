package scalaz.reactive
import scalaz.Monoid
import scalaz.zio.{IO, RTS}

case class Event[A](value: Future[Reactive[A]]) extends  RTS { self =>

  def merge(v: Event[A]): Event[A] = { // if Event[+A]: covariant type A occurs in contravariant position

    val winner: IO[Nothing, Either[(Time, Reactive[A]), (Time, Reactive[A])]] =
      self.value.force.map(Left(_)).race(v.value.force.map(Right(_)))

    val io: IO[Nothing, Future[Reactive[A]]] = winner.map(_ match {
      case Left((t, r)) =>
         Future((t, Reactive(r.head, r.tail)))
      case Right((t, r)) =>
         Future((t, Reactive(r.head, r.tail)))
    })

    Event(unsafeRun(io)) // That is not good
  }

  def map[B](f: A => B): Event[B] =
    Event(value.map(_.map(f)))

  implicit def monoidEvent[E, A]: Monoid[Event[A]] =
    new Monoid[Event[A]] {

      override def zero: Event[A] = Event(Future.Never)

      override def append(e1: Event[A], e2: => Event[A]): Event[A] =
        e1.merge(e2)
    }

  // TODO monoid
}
