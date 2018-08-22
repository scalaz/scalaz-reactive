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

  def flatMap[B](f: A => Event[B]): Event[B] = ???

  def filter(f: A => Boolean): Event[A] =
    Event(value.flatMap {
      case (t, r) => if (f(r.head)) IO.now((t, r)) else r.tail.filter(f).value
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
  def accumE[A](a: A)(e: Event[A => A]): Event[A] = {
    def h(r: Reactive[A => A]): Reactive[A] = accumR(r.head(a))(r.tail)
    Event(e.value.map { case (t, r) => (t, h(r)) })
  }

}
