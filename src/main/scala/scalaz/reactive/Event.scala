package scalaz.reactive
import scalaz.Monoid
import scalaz.reactive.Future._

case class Event[+A](value: Future[Reactive[A]]) { self =>

  def merge[AA >: A](v: Event[AA]): Event[AA] = Event(self.value.race(v.value))

  def map[B](f: A => B): Event[B] =
    Event(value.map { case (t, r) => (t, r.map(f)) })

  implicit def monoidEvent[E, A]: Monoid[Event[A]] =
    new Monoid[Event[A]] {

      override def zero: Event[A] = Event(Future.Never)

      override def append(e1: Event[A], e2: => Event[A]): Event[A] =
        e1.merge(e2)
    }

}
