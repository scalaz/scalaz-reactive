package scalaz.reactive
import scalaz.Monoid
import scalaz.reactive.Future._
import scalaz.zio.{ Fiber, IO }

import scala.concurrent.duration.Duration

case class Event[+A](value: Future[Reactive[A]]) { self =>
  def delay(interval: Duration): Event[A] = Event(value.delay(interval))

  def merge[AA >: A](v: Event[AA]): Event[AA] = {

    case class Outcome(value: (Time, Reactive[AA]), loser: Fiber[Void, (Time, Reactive[AA])])

    val futureReactive: IO[Void, (Time, Reactive[AA])] = self.value
      .raceWith(v.value)(
        (a, f) => IO.now(Outcome(a, f)),
        (a, f) => IO.now(Outcome(a, f))
      )
      .map {
        case Outcome((time, reactive), loser) =>
          val head: AA           = reactive.head
          val winTail: Event[AA] = reactive.tail
          (time, Reactive(head, winTail.merge(Event(loser.join))))
      }
    Event(futureReactive)

  }

  def map[B](f: A => B): Event[B] =
    Event(value.map { case (t, r) => (t, r.map(f)) })

  implicit def monoidEvent[E, A]: Monoid[Event[A]] =
    new Monoid[Event[A]] {

      override def zero: Event[A] = Event(Future.Never)

      override def append(e1: Event[A], e2: => Event[A]): Event[A] =
        e1.merge(e2)
    }

}
