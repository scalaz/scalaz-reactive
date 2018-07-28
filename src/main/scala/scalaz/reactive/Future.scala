package scalaz.reactive

import scalaz.{Applicative, Functor, Monad, Monoid, Order}
import scalaz.zio.IO
import Time.NegInf

case class Future[+E, +A](force: IO[E, (Time, A)])

object Future extends FutureInstances0 {

  def point[E, A](a: => A): Future[E, A] =
    Future(IO.point(NegInf -> a))

  def liftF[E, A](force: IO[E, (Time, A)]): Future[E, A] =
    Future(force)
}

trait FutureInstances0 extends FutureInstances1 {

  implicit def functorFuture[E]: Functor[Future[E, ?]] =
    new Functor[Future[E, ?]] {
      override def map[A, B](fa: Future[E, A])(f: A => B): Future[E, B] =
        Future(fa.force.map { case (time, a) => (time, f(a)) })
    }

  implicit def monoidFuture[E, A]: Monoid[Future[E, A]] =
    new Monoid[Future[E, A]] {
      override def zero: Future[E, A] = Future(IO.never)
      override def append(f1: Future[E, A], f2: => Future[E, A]): Future[E, A] =
        Future(f1.force race f2.force)
    }
}

trait FutureInstances1 extends FutureInstances2 {

  implicit def applicativeFuture[E]: Applicative[Future[E, ?]] =
    new Applicative[Future[E, ?]] {
      override def point[A](a: => A): Future[E, A] = Future(IO.point(NegInf -> a))
      override def ap[A, B](fa: => Future[E, A])(f: => Future[E, A => B]): Future[E, B] =
        Future(f.force.par(fa.force).map { case ((t1, fa0), (t2, a0)) => (Order[Time].max(t1, t2), fa0(a0)) })
    }
}

trait FutureInstances2 {

  implicit def monadFuture[E]: Monad[Future[E, ?]] =
    new Monad[Future[E, ?]] {
      override def point[A](a: => A): Future[E, A] = Future(IO.point(NegInf -> a))
      override def bind[A, B](fa: Future[E, A])(f: A => Future[E, B]): Future[E, B] =
        Future(fa.force.flatMap(a => f(a).force))
    }
}