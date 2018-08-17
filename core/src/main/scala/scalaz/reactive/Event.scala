package scalaz.reactive
import scalaz.Monoid
import scalaz.reactive.Future._
import scalaz.zio.Errors.LostRace
import scalaz.zio.IO

case class Event[+A](value: Future[Reactive[A]]) { self =>

  def merge[AA >: A](v: Event[AA]): Event[AA] = {
    val selfv: IO[Void, (Time, Reactive[AA])] = value
    val otherv: IO[Void, (Time, Reactive[AA])] = v.value

    case class Outcome(value: (Time, Reactive[AA]),
                       winner: Future[Reactive[AA]],
                       loser: Future[Reactive[AA]])
    val outcome: IO[Void, Outcome] = selfv.raceWith(v.value)(
      (a, fiber) =>
        fiber
          .interrupt(LostRace(Right(fiber)))
          .const(Outcome(a, selfv, otherv)),
      (a, fiber) =>
        fiber.interrupt(LostRace(Left(fiber))).const(Outcome(a, otherv, selfv))
    )
    val futureReactive: IO[Void, (Time, Reactive[AA])] = outcome.flatMap {
      outcome =>
        val winEvent: Event[AA] = Event(outcome.winner)
        val looseEvent: Event[AA] = Event(outcome.loser)
        val head: AA = outcome.value._2.head
        val tail: IO[Void, Event[AA]] = winEvent.value.map {
          case (_, r) => r.tail
        }
        val r: Reactive[AA] = Reactive(head, winEvent.merge(looseEvent))

      val zz: IO[Void, (Time, Reactive[AA])] = tail.map{ tailEvent => {
        val xx: Event[AA] = tailEvent.merge(looseEvent)
        val r = Reactive( head, xx)
        (outcome.value._1, r)
      }}
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
