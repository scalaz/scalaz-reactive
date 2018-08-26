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
      `TimeFun.ap` holds identity law. ${laws.apIdentityLaw}
      `TimeFun.ap` holds homomorphism law. ${laws.apHomomorphismLaw}
      `TimeFun.ap` holds interchange law. ${laws.apInterchangeLaw}
      `TimeFun.ap` holds derived map law. ${laws.apDerivedMapLaw}
    """

  // generate values for the a:A
  def aGen: Gen[Long] =
    Gen.choose(-100, 100)

  def timeGen: Gen[T] =
    for {
      t <- aGen
    } yield T(t)

  def faGen =
    for {
      v   <- aGen
      k   <- aGen
      fun <- Gen.oneOf(K(v), Fun(_.value * k * v))
    } yield fun

  def abGen: Gen[Long => Long] =
    for {
      k <- aGen
    } yield (l: Long) => l * k

  def fabGen: Gen[TimeFun[Long => Long]] =
    for {
      k <- aGen
    } yield K((l: Long) => l + k)

  def eqGen: Gen[TimeFun[Long] => Long] =
    for {
      l <- Gen.choose[Long](-100, 100)
      t = T(l)
    } yield (tf: TimeFun[Long]) => tf.apply(t)

  // TODO: extract this to a separate class
  class ApplicativeLaws[F[_], A](
    applicative: Applicative[F],
    aGen: Gen[A],
    faGen: Gen[F[A]],
    abGen: Gen[A => A],
    fabGen: Gen[F[A => A]],
    valueForEqGen: Gen[F[A] => A]) {

    implicit class Equalable(v: F[A]) {
      def valueForEq(f: F[A] => A) = f(v)
    }

    // ap(fa)(point(_)) == fa
    def apIdentityLaw = forAll(faGen, valueForEqGen) { (fa, v) =>
      applicative
        .ap(fa)(applicative.point(identity[A](_)))
        .valueForEq(v) must beEqualTo(fa.valueForEq(v))
    }

    // ap(point(a))(point(ab)) == point(ab(a))
    def apHomomorphismLaw = forAll(aGen, abGen) { (a, ab) =>
      applicative
        .ap(applicative.point(a))(applicative.point(ab)) must
        beEqualTo(applicative.point(ab(a)))
    }

    // ap(point(a))(fab) == ap(fab)(point(_.apply(a)))
    def apInterchangeLaw = forAll(aGen, fabGen) { (a, fab) =>
      applicative.ap(applicative.point(a))(fab) must
        beEqualTo(
          applicative.ap(fab)(applicative.point((x: A => A) => x.apply(a)))
        )
    }

    //map(fa)(ab) == ap(fa)(point(ab))
    def apDerivedMapLaw = forAll(faGen, abGen, valueForEqGen) { (fa, ab, v) =>
      applicative.map(fa)(ab).valueForEq(v) must
        beEqualTo(applicative.ap(fa)(applicative.point(ab)).valueForEq(v))
    }
  }

  val laws =
    new ApplicativeLaws(Applicative[TimeFun], aGen, faGen, abGen, fabGen, eqGen)

}
