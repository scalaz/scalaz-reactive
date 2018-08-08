package scalaz.reactive

import scalaz.{ Applicative, Functor, Monad }

case class Reactive[A](head: A, tail: Event[A]) {

  def map[B](f: A => B): Reactive[B] =
    Reactive(f(head), tail.map(f))

  def ap[B](f: Reactive[A => B]): Reactive[B] =
    Reactive(
      f.head(head),
      Event(
        f.tail.value.map((f0: Reactive[A => B]) => ap(f0)) + tail.value
          .map((fa: Reactive[A]) => fa.ap(f))
      )
    )

  def flatMap[B](f: A => Reactive[B]): Reactive[B] = {
    val other                      = f(head)
    val tail1: Future[Reactive[B]] = other.tail.value
    val tail2: Future[Reactive[B]] = tail.value.flatMap(r => f(r.head).tail.value)
    Reactive(other.head, Event(tail1 + tail2))
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
