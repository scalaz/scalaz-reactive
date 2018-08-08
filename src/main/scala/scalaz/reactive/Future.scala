package scalaz.reactive

import scalaz.Scalaz._
import scalaz.reactive.Time.{NegInf, PosInf}
import scalaz.zio.IO
import scalaz.{Applicative, Functor, Monad, Monoid}

object Types {
  type Infallible[A] = IO[Nothing, A]
}

case class Future[+A](force: IO[Nothing, (Time, A)]) {

  def +[AA >: A](other: Future[AA]): Future[AA] =
    Future(force.flatMap {
      case (t1, a1) =>
        other.force.map {
          case (t2, a2) => if (t1 <= t2) (t1, a1) else (t2, a2)
        } //There should be IO.ap, is there?
    })

  def map[B](f: A => B): Future[B] =
    Future(force.map { case (t, a) => (t, f(a)) })

  def ap[B](f: => Future[A => B]): Future[B] =
    Future(force.flatMap {
      case (t, a) =>
        f.force
          .map { case (t2, f) => (t2.max(t), f(a)) } // There should be IO.ap, is there?
    })

  def flatMap[B](f: A => Future[B]): Future[B] = {
    Future(force.flatMap { case (_, a) => f(a).force }) // FIXME - what to do with the times
  }
}

object Future extends FutureInstances0 {

  def point[A](a: => A): Future[A] =
    Future((NegInf, a))

  def apply[A](force: => (Time, A)): Future[A] =
    Future(IO.point(force))

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

      override def bind[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = {
        Future(fa.force.flatMap { case (_, a) => f(a).force })
      }
    }
}
