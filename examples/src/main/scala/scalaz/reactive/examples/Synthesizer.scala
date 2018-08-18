package scalaz.reactive.examples

import scalaz.Scalaz._
import scalaz._
import scalaz.reactive._
import scalaz.zio.{ App, IO }

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

  def build(eKey: Event[Char]): Behaviour[Note] = {
    val ePitch = eKey
      .map(table.get(_))
      .filter(_.isDefined)
      .map(_.get) // implement flatten, or filterSome

    val eOctChange: Event[Octave => Octave] = eKey
      .map { k =>
        k match {
          case '+' => Some((x: Octave) => x + 1)
          case '-' => Some((x: Octave) => x - 1)
          case _   => None
        }
      }
      .filter(_.isDefined)
      .map(_.get)

    val bOctave: Behaviour[Octave] = Behaviour(Reactive(TimeFun.K(0), ???))
    val bPitch: Behaviour[Pitch]   = Behaviour(Reactive(TimeFun.K(PA), ???))

    (bOctave |@| bPitch) { Note.apply _ }
  }

  def run(args: List[String]): IO[Nothing, ExitStatus] =
    myAppLogic.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))

  case class Tick(name: String)

  def randomChar(lower: Char, upper: Char) =
    ((Math.random() * (upper - lower)).toInt + lower).toChar
  def randomChar = randomChar(' ', 128)

  def eKey(): Event[Char] =
    Event(IO.point { (Time.now, Reactive(randomChar, eKey())) }) // non FP here

  def myAppLogic: IO[Void, Unit] = {
    Sink[Note, Unit](n => IO.now(println(s"Note [${n.octave},${n.pitch}]")))
    ???
  }

}
