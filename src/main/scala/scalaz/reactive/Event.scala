package scalaz.reactive

case class Event[+E, +A](us: Future[E, A]) extends AnyVal
