package scalaz.reactive

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.matcher.MustMatchers
import org.specs2.{ScalaCheck, Specification}
import scalaz.Monoid
import scalaz.reactive.io.types.IO1

class FutureSpec
    extends Specification
    with ScalaCheck
    with FutureInstances
    with MustMatchers {

  import scalaz.reactive.io.instances._

  def is = "FutureSpec".title ^ s2"""
   Generate futures
      Resolves two exact futures. ${resolveTwoExactFutures}
    """

  // generate values for the a:A
  def timeGen: Gen[Time.T] =
    Gen.choose[Long](0, 100).map(t => Time.T(t))

  def exactFuture =
    for {
      t <- timeGen
    } yield Future[IO1, Int](t, 0)

  def resolveTwoExactFutures = forAll(exactFuture, exactFuture) { (f1, f2) =>
    val monoid: Monoid[Future[IO1, Int]] = monoidFuture[IO1, Int]
    monoid.append(f1, f2) must beEqualTo(monoid.append(f1, f2))
  }

}
