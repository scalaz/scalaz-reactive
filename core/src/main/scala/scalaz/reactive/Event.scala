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
 * has the same content as a future reactive value. This insight leads to a new representation
 * of functional events:
 *
 * {{{
 * -- for non-decreasing times
 * newtype Event a = Ev (Future (Reactive a))
 * }}}
 *
 * See more details in section 6 at http://conal.net/papers/push-pull-frp
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

  def flatMap[B](f: A => Event[B]): Event[B] = ???

  def filter(f: A => Boolean): Event[A] =
    Event(value.flatMap {
      case (t, r) => {
        if (f(r.head))
          IO.point((t, Reactive(r.head, r.tail.filter(f))))
        else
          r.tail.filter(f).value
      }
    })

  implicit def monoidEvent[E, A]: Monoid[Event[A]] =
    new Monoid[Event[A]] {

      override def zero: Event[A] = Event(Future.Never)

      override def append(e1: Event[A], e2: => Event[A]): Event[A] =
        e1.merge(e2)
    }

}

object Event {
  // chapter 12
  //accumR :: a → Event (a → a) → Reactive a
  def accumR[A](a: A)(e: Event[A => A]): Reactive[A] = Reactive(a, accumE(a)(e))

  //  accumE :: a → Event (a → a) → Event a
  def accumE[A](a: A)(e: Event[A => A]): Event[A] =
    Event(e.value.map { case (t, r) => (t, accumR(r.head(a))(r.tail)) })

  def joinMaybes[A](e: Event[Option[A]]): Event[A] = {
    val value: Future[Reactive[A]] = e.value.flatMap {
      case (t, Reactive(Some(head), (tail: Event[Option[A]]))) =>
        IO.point((t, Reactive(head, joinMaybes(tail))))
      case (_, Reactive(None, (tail: Event[Option[A]]))) =>
        joinMaybes(tail).value
    }
    Event(value)
  }
}
