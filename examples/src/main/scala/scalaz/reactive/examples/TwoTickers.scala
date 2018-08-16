package scalaz.reactive.examples

import scalaz.reactive.Future.Future
import scalaz.reactive.{ Event, Reactive, Time }
import scalaz.zio.{ App, IO }

import scala.concurrent.duration._
import scala.language.postfixOps

object TwoTickers extends App {

  def run(args: List[String]): IO[Nothing, ExitStatus] =
    myAppLogic.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))

  trait Tick
  object Tick extends Tick

  def tick: Future[Tick] = IO.now(Tick).delay(1 second).map(t => (Time(), t))

  def reactiveTicks(): IO[Void, (Time, Reactive[Tick])] =
    IO.point {
        println("tick")
        (Time(), Reactive(Tick, ticks()))
      }
      .delay(1 second)

  def ticks(): Event[Tick] = Event(reactiveTicks())

  def myAppLogic = scalaz.reactive.Sink[Tick, Unit](_ => IO.point(println("tick"))).sink(ticks())

}
