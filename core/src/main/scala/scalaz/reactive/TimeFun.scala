package scalaz.reactive

import scalaz.reactive.Time._
import scalaz.{ Applicative, Functor, Monad }
import scalaz.reactive.TimeFun.{ Fun, K }

sealed trait TimeFun[+A] {

  def apply: Time.T => A

  def map[B](f: A => B): TimeFun[B] =
    this match {
      case K(a)   => K(f(a))
      case Fun(g) => Fun(f.compose(g))
    }

  def ap[B](f: TimeFun[A => B]): TimeFun[B] =
    (f, this) match {
      case (K(g), K(a))   => K(g(a))
      case (K(g), Fun(f)) => Fun(g.compose(f))
      case (Fun(g), h)    => Fun(t => g(t)(h.apply(t)))
    }

  def flatMap[B](f: A => TimeFun[B]): TimeFun[B] =
    this match {
      case K(a)   => f(a)
      case Fun(g) => Fun(t => f(g(t)).apply(t))
    }
}

object TimeFun extends TimeFunFunctions with TimeFunInstances {

  case class K[+A](a: A) extends TimeFun[A] {

    def apply: T => A = _ => a
  }

  case class Fun[+A](f: T => A) extends TimeFun[A] {

    def apply: T => A = f
  }

}

trait TimeFunFunctions {

  def point[A](a: => A): TimeFun[A] =
    K(a)
}

trait TimeFunInstances {

  implicit def functorTimeFun: Functor[TimeFun] = new Functor[TimeFun] {
    override def map[A, B](fa: TimeFun[A])(f: A => B): TimeFun[B] =
      fa.map(f)
  }

  implicit def applicativeTimeFun: Applicative[TimeFun] =
    new Applicative[TimeFun] {

      override def point[A](a: => A): TimeFun[A] = TimeFun.point(a)

      override def ap[A, B](fa: => TimeFun[A])(f: => TimeFun[A => B]): TimeFun[B] =
        fa.ap(f)
    }

  def monadTimeFun: Monad[TimeFun] =
    new Monad[TimeFun] {

      override def point[A](a: => A): TimeFun[A] = TimeFun.point(a)

      override def bind[A, B](fa: TimeFun[A])(f: A => TimeFun[B]): TimeFun[B] =
        fa.flatMap(f)
    }
}
