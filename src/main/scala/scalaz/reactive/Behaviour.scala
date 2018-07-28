package scalaz.reactive

import scalaz.{Applicative, Functor}

case class Behaviour[+A](at: Time => A) extends AnyVal

object Behaviour extends BehaviourInstances0

trait BehaviourInstances0 extends BehaviourInstances1 {

  implicit val functorBehaviour: Functor[Behaviour] =
    new Functor[Behaviour] {
      override def map[A, B](fa: Behaviour[A])(f: A => B): Behaviour[B] =
        Behaviour(f compose fa.at)
    }
}

trait BehaviourInstances1 {

  implicit val applicativeBehaviour: Applicative[Behaviour] =
    new Applicative[Behaviour] {
      override def point[A](a: => A): Behaviour[A] = Behaviour(_ => a)
      override def ap[A, B](fa: => Behaviour[A])(f: => Behaviour[A => B]): Behaviour[B] =
        Behaviour(t => (f at t)(fa at t))
    }
}
