package scalaz.reactive
import scalaz.Monoid
import scalaz.reactive.Future._
import scalaz.zio.{ Fiber, IO }

import scala.concurrent.duration.Duration

/**
 * Semantically, an Event is time-ordered lists of future values, where a future value 
 * is a time/value pair: [(t0 , a0 ), (t1 , a1 ), ...]. If such an occurrence list is 
 * nonempty, another view on it is as a time t0, together with a reactive value having 
 * initial value a0 and event with occurrences [(t1,a1),...]. If the occurrence list is 
 * empty, then we could consider it to have initial time ∞ (maxBound), and reactive value
 * of ⊥. Since a future value is a time and value, it follows that an event (empty or nonempty) 
 * has the same content as a future reactive value (Push-Pull FRP section 6).
 */
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
