//package scalaz.reactive
//
//import scalaz.{ Applicative, Functor }
//
//case class Behaviour[A](value: Reactive[TimeFun[A]]) extends AnyVal {
//
//  def map[B](f: A => B): Behaviour[B] =
//    Behaviour.functorBehaviour.map(this)(f)
//
//  def ap[B](f: Behaviour[A => B]): Behaviour[B] =
//    Behaviour.applicativeBehaviour.ap(this)(f)
//}
//
//object Behaviour extends BehaviourInstances {
//
//  def point[A](a: => A): Behaviour[A] =
//    applicativeBehaviour.point(a)
//}
//
//trait BehaviourInstances extends TimeFunInstances {
//
//  val reactiveTimeFunFunctor =
//    Functor[Reactive].compose(Functor[TimeFun])
//
//  implicit def functorBehaviour: Functor[Behaviour] =
//    new Functor[Behaviour] {
//
//      override def map[A, B](fa: Behaviour[A])(f: A => B): Behaviour[B] =
//        Behaviour(reactiveTimeFunFunctor.map(fa.value)(f))
//    }
//
//  val timeFunApplicative = Applicative[TimeFun]
//
//  val reactiveTimeFunApplicative =
//    Applicative[Reactive].compose(Applicative[TimeFun])
//
//  implicit def applicativeBehaviour: Applicative[Behaviour] =
//    new Applicative[Behaviour] {
//
//      override def point[A](a: => A): Behaviour[A] =
//        Behaviour(reactiveTimeFunApplicative.point(a))
//
//      override def ap[A, B](fa: => Behaviour[A])(f: => Behaviour[A => B]): Behaviour[B] =
//        Behaviour(reactiveTimeFunApplicative.ap(fa.value)(f.value))
//    }
//}
