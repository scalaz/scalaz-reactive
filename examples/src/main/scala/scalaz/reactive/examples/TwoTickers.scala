package scalaz.reactive.examples

import scalaz.reactive._
import scalaz.zio.{App, IO}

import scala.concurrent.duration._
import scala.language.postfixOps

object TwoTickers extends App {

  def run(args: List[String]): IO[Nothing, ExitStatus] =
    myAppLogic.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))

  case class Tick(value: String)

  def ticks(interval: Duration, name: String): Event[Tick] =
    Event(
      IO.point { (Time.now, Reactive(Tick(name), ticks(interval, name))) }
        .delay(interval)
    )

  def myAppLogic =
    Sink[Tick, Unit](t => IO.now(println(s"tick ${t.value}")))
      .sink(
        ticks(1 second, "a")
          .merge(ticks(1.5 second, "b"))
      )

}
