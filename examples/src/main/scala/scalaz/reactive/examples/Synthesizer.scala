package scalaz.reactive.examples

import scalaz.reactive._
import scalaz.zio.{App, IO}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Example from https://wiki.haskell.org/FRP_explanation_using_reactive-banana
  */
object Synthesizer extends App {

  type Octave = Int
  sealed trait Pitch
  object PA extends Pitch
  object PB extends Pitch
  object PC extends Pitch
  object PD extends Pitch
  object PE extends Pitch
  object PF extends Pitch
  object PG extends Pitch
  case class Note(octave: Octave, pitch: Pitch)

  val table = Map('a' -> PA, 'b' -> PB, 'g' -> PG)

  def build(eKey: Event[Char]) = {
    val ePitch = eKey
        .map(table.get(_))
        .filter(_.isDefined)
        .map(_.get) // implement flatten, or filterSome

    val eOctChange : Event[Octave => Octave] = eKey.map { k =>
      k match {
        case '+' => Some((x: Octave) => x + 1)
        case '-' => Some((x: Octave) => x - 1)
        case _   => None
      }
    }.filter(_.isDefined)
      .map(_.get)

    val bOctave: Behaviour[Octave] = ???
    val bPitch: Behaviour[Pitch] = ???

    val bNote = bOctave <*> bPitch


  }


  def run(args: List[String]): IO[Nothing, ExitStatus] =
    myAppLogic.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))

  case class Tick(name: String)

  def ticks(interval: Duration, name: String): Event[Tick] =
    Event(IO.point {
      (Time.now, Reactive(Tick(name), ticks(interval, name).delay(interval)))
    })

  def myAppLogic: IO[Void, Unit] =
    Sink[Tick, Unit](t => IO.now(println(s"tick ${t.name}")))
      .sink(
        ticks(0.2 second, "a")
          .merge(ticks(0.4 second, "b"))
      )

}
