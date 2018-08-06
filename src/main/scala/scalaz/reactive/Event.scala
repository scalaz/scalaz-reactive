package scalaz.reactive

case class Event[+A](value: Future[Reactive[A]]) extends AnyVal {

  def map[B](f: A => B): Event[B] =
    Event(value.map(_.map(f)))

  // TODO monad

  // TODO monoid
}
