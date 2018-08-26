package scalaz.reactive

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.{ ScalaCheck, Specification }
import scalaz._
import scalaz.reactive.Time.T
import scalaz.reactive.TimeFun._

class TimeFunSpec extends Specification with ScalaCheck with TimeFunInstances {

  def is = "TimeFunSpec".title ^ s2"""
   Generate a mix of K and Fun TimeFuns
      `TimeFun.ap composes. $apComposes
      `TimeFun.ap` holds identity law. $holdsIdentity
    """

  def longGen: Gen[Long] =
    Gen.choose(-100, 100)

  def timeGen: Gen[T] =
    for {
      t <- longGen
    } yield T(t)

  def funGen =
    for {
      v   <- longGen
      k   <- longGen
      fun <- Gen.oneOf(K(v), Fun(_.value * k * v))
    } yield fun

  val timefunApplicative = Applicative[TimeFun]

  def apComposes = forAll(funGen, funGen, timeGen) { (f1, f2, t) =>
    val composed = timefunApplicative.apply2(f1, f2)(_ + _)

    composed.apply(t) mustEqual f1.apply(t) + f2.apply(t)
  }

  def holdsIdentity = forAll(funGen, timeGen) { (f, t) =>
    timefunApplicative.ap(f)(K(identity[Long](_))).apply(t) must beEqualTo(f.apply(t))
  }
}
