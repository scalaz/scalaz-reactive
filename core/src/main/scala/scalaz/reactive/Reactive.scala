package scalaz.reactive

import scalaz.{ Applicative, Functor, Monad }
import Future._

/**
 * A reactive value is like a reactive behaviour but is restricted to changing discretely. Its
 * meaning is a step function defined by an initial value, `head`, and discrete changes, `tail`.
 * Each discrete change is defined by a time and a value, which correspond exactly to an FRP `Event`.
 * (See section 5.2 of Elliott's Push-Pull FRP paper)
 */
case class Reactive[+A](head: A, tail: Event[A]) {

  def map[B](f: A => B): Reactive[B] =
    Reactive(f(head), tail.map(f))

  def ap[B](f: Reactive[A => B]): Reactive[B] = ??? // 7.1.2: Applicative is more challenging. :-(

  /* 5.2
   rat (r >>= k) = rat r >>= rat ◦ k
    = λt → (rat ◦ k) (rat r t) t
    = λt → rat (k (rat r t)) t
  **/
  def flatMap[B](f: A => Reactive[B]): Reactive[B] = {
    val other: Reactive[B]             = f(head)
    val otherTail: Future[Reactive[B]] = other.tail.value
    val thisTail: Future[Reactive[B]]  = tail.value.flatMap { case (_, r) => f(r.head).tail.value }
    Reactive(other.head, Event(Future.merge(thisTail, otherTail)))
  }
}

object Reactive extends ReactiveInstances {

  def point[A](a: => A): Reactive[A] =
    Reactive(a, Event(Future.Never))
}

trait ReactiveInstances {

  implicit def functorReactive: Functor[Reactive] =
    new Functor[Reactive] {

      override def map[A, B](fa: Reactive[A])(f: A => B): Reactive[B] =
        fa.map(f)
    }

  implicit def applicativeReactive: Applicative[Reactive] =
    new Applicative[Reactive] {

      override def point[A](a: => A): Reactive[A] = Reactive.point(a)

      override def ap[A, B](fa: => Reactive[A])(f: => Reactive[A => B]): Reactive[B] =
        fa.ap(f)
    }

  implicit def monadReactive: Monad[Reactive] =
    new Monad[Reactive] {

      override def point[A](a: => A): Reactive[A] = Reactive.point(a)

      override def bind[A, B](fa: Reactive[A])(f: A => Reactive[B]): Reactive[B] =
        fa.flatMap(f)
    }
}
