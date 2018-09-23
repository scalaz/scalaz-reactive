package scalaz.reactive

import org.scalacheck.Gen
import org.specs2.{ScalaCheck, Specification}
import scalaz._
import scalaz.reactive.domain.TestDomain._
import scalaz.reactive.Time.T
import scalaz.reactive.laws.ApplicativeLaws
import scalaz.zio.IO

class ReactiveSpec
    extends Specification
    with ScalaCheck
    with ReactiveInstances {

  // laws.apIdentityLaw fails, without ever reaching Reactive.ap function

  def is = "ReactiveInstances".title ^ s2"""
   Generate a mix of K and Fun TimeFuns
      `Reactive.ap` holds identity law. ${laws.apIdentityLaw}
    """

  def aGen: Gen[A] =
    Gen.choose(-5L, 6L).map(i => if (i < 5) Right(i) else STOP)

  val stopEvent: Event[A] =
    Event(IO.sync { (T(0), Reactive(STOP, stopEvent)) })

  def genStopEvent: Gen[Event[A]] =
    Gen.oneOf(List(stopEvent)) // how to choose from 1?

  def faGenEvent: Gen[Event[A]] =
    for {
      r <- faGenReactive
    } yield Event(IO.sync(((T(0), r))))

  def faGenReactive: Gen[Reactive[A]] =
    for {
      head <- aGen
      tail <- if (head.isLeft) genStopEvent else faGenEvent
    } yield Reactive(head, tail)

  def abGen: Gen[A => A] = ???

  def fabGenBehaviour: Gen[Reactive[A => A]] = ???

  val laws =
    new ApplicativeLaws(
      Applicative[Reactive],
      aGen,
      faGenReactive,
      abGen,
      fabGenBehaviour,
      valueForComparisonReactive
    )

}
