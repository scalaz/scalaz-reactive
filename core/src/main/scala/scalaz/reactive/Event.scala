package scalaz.reactive
import scalaz.Monoid
import scalaz.reactive.Future._
import scalaz.zio.Errors.LostRace
import scalaz.zio.IO

case class Event[+A](value: Future[Reactive[A]]) { self =>

  def merge[AA >: A](v: Event[AA]): Event[AA] = {

    case class Outcome(
      value: (Time, Reactive[AA]),
      winner: Future[Reactive[AA]],
      loser: Future[Reactive[AA]])

    val ioOutcome: IO[Void, Outcome] = value.raceWith(v.value)(
      (a, fiber) =>
        fiber
          .interrupt(LostRace(Right(fiber)))
          .const(Outcome(a, value, v.value)),
      (a, fiber) => fiber.interrupt(LostRace(Left(fiber))).const(Outcome(a, v.value, value))
    )
    val futureReactive: IO[Void, (Time, Reactive[AA])] = ioOutcome.flatMap { outcome =>
      val winEvent: Event[AA]   = Event(outcome.winner)
      val looseEvent: Event[AA] = Event(outcome.loser)
      val head: AA              = outcome.value._2.head
      val winTail: IO[Void, Event[AA]] = winEvent.value.map {
        case (_, r) => r.tail
      }

      winTail.map { tailEvent =>
        (outcome.value._1, Reactive(head, tailEvent.merge(looseEvent)))
      }
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
