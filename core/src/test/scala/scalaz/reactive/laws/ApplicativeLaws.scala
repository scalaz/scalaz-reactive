package scalaz.reactive.laws
import org.scalacheck.{Gen, Prop}
import org.scalacheck.Prop.forAll
import org.specs2.matcher.{MatchResult, MustMatchers}
import scalaz.Applicative

/**
  * To run property based testson the Applictive laws on F[_]
  *
  * @param applicative
  *
  */
class ApplicativeLaws[F[_], A, B](applicative: => Applicative[F],
                                  aGen: => Gen[A],
                                  faGen: => Gen[F[A]],
                                  abGen: => Gen[A => A],
                                  fabGen: => Gen[F[A => A]],
                                  fComp: => F[A] => B)(
  implicit
  pfa: MatchResult[F[A]] => Prop,
  pb: MatchResult[B] => Prop
) extends MustMatchers {

  implicit class ExtractValueForEq(v: F[A]) {
    def valueForEq: B = fComp(v)
  }

  // Identity law: ap(fa)(point(_)) == fa
  def apIdentityLaw = forAll(faGen) { fa =>
    println(s"Checking identity of $fa")
    val right: B = fa.valueForEq
    val left: B = applicative.ap(fa)(applicative.point(identity[A](_))).valueForEq
    left must beEqualTo(right)
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
  def apDerivedMapLaw = forAll(faGen, abGen) { (fa, ab) =>
    applicative.map(fa)(ab).valueForEq must
      beEqualTo(applicative.ap(fa)(applicative.point(ab)).valueForEq)
  }
}
