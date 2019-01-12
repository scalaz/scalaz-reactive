package scalaz.reactive

import java.text.SimpleDateFormat

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.matcher.MustMatchers
import org.specs2.{ScalaCheck, Specification}
import scalaz._
import Scalaz._
import scalaz.reactive.io.types.IO1
import scalaz.zio.{IO, RTS}

import scala.concurrent.duration._

class FutureSpec
    extends Specification
    with ScalaCheck
    with FutureInstances
    with MustMatchers {

  import scalaz.reactive.io.instances._

  val rts = new RTS {}

  val DateFormat = "HH:mm:ss.SSS"
  def now = new SimpleDateFormat(DateFormat).format(System.currentTimeMillis())

  def is = "FutureSpec".title ^ s2""" 
   Appends two slow futures. ${appendTwoSlowFutures}
   Appends two exact futures. ${appendTwoExactFutures}
   Appends exact + slow futures. ${appendExactToSlowFutures}
    """

  // generate values for the a:A
  def timeGen: Gen[Time.T] =
    Gen.choose[Long](0, 100).map(t => Time.T(t))

  def exactFuture =
    for {
      t <- timeGen
    } yield Future[IO1, Int](t, 0)

  def slowFuture =
    for {
      num <- Gen.choose[Int](0, 100)
      delay <- Gen.choose[Int](300, 500)
    } yield
      Future[IO1, Int](
        IO.sync(println(s"$now will wait $delay for value $num"))
          .flatMap(_ => {
            IO.point(())
              .delay(delay milli)
              .map(_ => println(s"$now waited $delay for value $num"))
          })
          .map(_ => TimedValue(Improving.now, _ => num))
      )

  def appendTwoExactFutures = forAll(exactFuture, exactFuture) {
    (f1: Future[IO1, Int], f2: Future[IO1, Int]) =>
      rts.unsafeRun((f1 |+| f2).ftv).t.exact must beEqualTo(
        rts.unsafeRun((f2 |+| f1).ftv).t.exact
      )
  }

  def appendTwoSlowFutures = forAll(slowFuture, slowFuture) {
    (f1: Future[IO1, Int], f2: Future[IO1, Int]) =>
      println(s"Running $f1 and $f2")
      val val1 = rts.unsafeRun((f1 |+| f2).ftv).get
      println(s"computed val1: $val1")
      val val2 = rts.unsafeRun((f2 |+| f1).ftv).get
      println(s"computed val2: $val2")
      val1 must beEqualTo(val2)
  }

  def appendExactToSlowFutures = forAll(exactFuture, slowFuture) {
    (ef: Future[IO1, Int], sf: Future[IO1, Int]) =>
      println(s"Running $ef and $sf")

      val val1 = rts.unsafeRun((ef |+| sf).ftv).get
      println(s"computed val1: $val1")
      val val2 = rts.unsafeRun((sf |+| ef).ftv).get
      println(s"computes val2: $val2")
      val1 must beEqualTo(val2)
  }

}
