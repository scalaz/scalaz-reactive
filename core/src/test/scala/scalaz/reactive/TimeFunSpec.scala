package scalaz.reactive

import scalaz._
import Scalaz._

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.{ ScalaCheck, Specification }
import scalaz.reactive.TimeFun._

class TimeFunSpec extends Specification with ScalaCheck with TimeFunInstances {

  def is = "TimeFunSpec".title ^ s2"""
   Generate a list of K timefuncs
      `TimeFunc.ap` applies function to 2 integers. $t1
      `TimeFunc.ap` holds identity law. $t2
    """

  def intGen: Gen[Int] =
    Gen.choose(-1000, 1000)

  val timefuncApplicative = Applicative[TimeFun]

  def t1 = forAll(intGen, intGen) { (i1, i2) =>
    //FIXME: why is asInstanceOf needed here? Without it, - no implicits found for parameter F: Apply[K] ???
    val res2 = ^(K(i1).asInstanceOf[TimeFun[Int]], K(i2))(_ * _)
    res2 must beEqualTo(K(i1 * i2))
  }

  def t2 = forAll(intGen) { (i) =>
    val k = K(i)
    timefuncApplicative.ap(k)(K(identity[Int](_))) must beEqualTo(k)
  }
}
