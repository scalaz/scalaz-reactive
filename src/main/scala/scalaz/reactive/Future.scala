package scalaz.reactive

import scalaz.Scalaz._
import scalaz.reactive.Time.{NegInf, PosInf}
import scalaz.{Applicative, Functor, Monad, Monoid}

case class Future[+A](time: () => Time, force: () => A) {

  def + [AA >: A](other: Future[AA]): Future[AA] =
    Future(() => time() min other.time(), () => if(time() <= other.time()) force() else other.force())

  def map[B](f: A => B): Future[B] =
    Future(() => time(), () => f(force()))

  def ap[B](f: => Future[A => B]): Future[B] =
    Future(() => time() max f.time(), () => f.force()(force()))

  def flatMap[B](f: A => Future[B]): Future[B] =
    f(force())
}

object Future extends FutureInstances0 {

  def point[A](a: => A): Future[A] =
    Future(() => NegInf, () => a)

  def apply[A](time: => Time, a: => A): Future[A] =
    Future[A](() => time, () => a)

  def apply[A](force: => (Time, A)): Future[A] =
    Future[A](() => force._1, () => force._2)

  def Never[A]: Future[A] = Future[A](PosInf -> null.asInstanceOf[A])
}

trait FutureInstances0 extends FutureInstances1 {

  implicit def functorFuture: Functor[Future] =
    new Functor[Future] {

      override def map[A, B](fa: Future[A])(f: A => B): Future[B] =
        fa.map(f)
    }

  implicit def monoidFuture[E, A]: Monoid[Future[A]] =
    new Monoid[Future[A]] {

      override def zero: Future[A] = Future.Never

      override def append(f1: Future[A], f2: => Future[A]): Future[A] =
        f1 + f2
    }
}

trait FutureInstances1 extends FutureInstances2 {

  implicit def applicativeFuture: Applicative[Future] =
    new Applicative[Future] {

      override def point[A](a: => A): Future[A] =
        Future.point(a)

      override def ap[A, B](fa: => Future[A])(f: => Future[A => B]): Future[B] =
        fa.ap(f)
    }
}

trait FutureInstances2 {

  implicit def monadFuture: Monad[Future] =
    new Monad[Future] {

      override def point[A](a: => A): Future[A] =
        Future.point(a)

      override def bind[A, B](fa: Future[A])(f: A => Future[B]): Future[B] =
        f(fa.force())
    }
}