package scalaz.reactive

import org.scalacheck.Gen
import org.specs2.{ ScalaCheck, Specification }
import scalaz._
import scalaz.reactive.Time.T
import scalaz.reactive.TimeFun._
import scalaz.reactive.laws.ApplicativeLaws

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

  val laws =
    new ApplicativeLaws(Applicative[TimeFun], aGen, faGen, abGen, fabGen, eqGen)

}
