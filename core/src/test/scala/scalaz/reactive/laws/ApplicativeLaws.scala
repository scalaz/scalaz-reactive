package scalaz.reactive.laws
import org.scalacheck.{ Gen, Prop }
import org.scalacheck.Prop.forAll
import org.specs2.matcher.{ MatchResult, MustMatchers }
import scalaz.Applicative

class ApplicativeLaws[F[_], A](
  applicative: Applicative[F],
  aGen: Gen[A],
  faGen: Gen[F[A]],
  abGen: Gen[A => A],
  fabGen: Gen[F[A => A]],
  valueForEqGen: Gen[F[A] => A]
)(implicit
  pa: MatchResult[A] => Prop,
  pfa: MatchResult[F[A]] => Prop)
    extends MustMatchers {

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
