package scalaz.tagless

import scalaz._


object instances {

  implicit def monadReactive[F[_]](implicit frp: Frp[F],
                                   monad: Monad[F]): Monad[Reactive[F, ?]] = {
    type R[A] = Reactive[F, A]

    new Monad[R] {
      override def bind[A, B](fa: R[A])(f: A => R[B]): R[B] = {
        val other: Reactive[F, B] = f(fa.head)
        val otherTail: F[Reactive[F, B]] = other.tail.value
        val thisTail: F[Reactive[F, B]] = monad.bind(fa.tail.value) { r =>
          f(r.head).tail.value
        }
        val merged: F[Reactive[F, B]] = frp.merge(thisTail, otherTail)
        Reactive(other.head, Event(merged))
      }

      override def point[A](a: => A): R[A] =
        Reactive[F, A](a, frp.never)
    }
  }

}


