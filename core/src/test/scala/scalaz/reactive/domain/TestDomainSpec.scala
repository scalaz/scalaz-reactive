package scalaz.reactive.domain
import org.specs2.specification.core.SpecStructure
import org.specs2.{ScalaCheck, Specification}
import scalaz.reactive.Time.T
import scalaz.reactive.domain.TestDomain.A
import scalaz.reactive.{Event, Reactive}
import scalaz.zio.IO

class TestDomainSpec extends Specification with ScalaCheck {
  override def is: SpecStructure = "TestDomain".title ^ s2"""
      Reactive values get reduced correctly. ${testReduceReactive}

    """

  def testReduceReactive = {
    def r: Reactive[A] =
      Reactive[A](
        Right(1),
        Event(
          IO.sync(
            (
              T(0),
              Reactive(
                Right(2),
                Event(
                  IO.sync(
                    (T(0), Reactive(TestDomain.STOP, Event(IO.sync((T(0), r)))))
                  )
                )
              )
            )
          )
        )
      )

    TestDomain.valueForComparisonReactive(r) must beEqualTo(List(1, 2))

  }
}
