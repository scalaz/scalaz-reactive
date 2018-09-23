package scalaz.reactive.domain
import scalaz.reactive.Reactive
import scalaz.zio.{IO, RTS}

object TestDomain extends RTS {

  type A = Either[Stop, Long] // type to use in specification
  case class Stop()

  val STOP = Left(Stop())

  /*
  Compare two Reactives for equality. reduce reactive to list of its elements
   */
  def valueForComparisonReactive: Reactive[A] => List[Long] =
    r => {
      println(s"cmpReactive: reactive = $r")
      val toReduce = reduce(List(), r)
      println(s"cmpReactive: toReduce = $toReduce")
      unsafeRun(toReduce)
    }

  def reduce(ls: List[Long], r: Reactive[A]): IO[Void, List[Long]] = {
    println(s"Reducing $r")
    r match {
      case Reactive(Left(Stop()), _) => { println(s"Encountered STOP - result is $ls"); IO.now(ls)}
      case Reactive(Right(l), tail) => {
        println("flatmapping")
        tail.value.flatMap {
          case (_, rr: Reactive[A]) => { println("inside flatmap"); reduce(ls :+ l, rr) }
        }
      }
    }
  }

}
