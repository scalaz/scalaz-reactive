package examples

import scalaz._
import Scalaz._
import scalaz.reactive._
import scalaz.reactive.Ops._

// The Synthesizer example from
// https://wiki.haskell.org/FRP_explanation_using_reactive-banana
object Synthesizer {

  type Octave = Int

  sealed trait Pitch

  object PA extends Pitch

  object PB extends Pitch

  object PC extends Pitch

  object PD extends Pitch

  object PE extends Pitch

  object PF extends Pitch

  object PG extends Pitch

  val table =
    Map(('a' -> PA), ('b' -> PB), ('c' -> PC), ('d' -> PD), ('e' -> PE), ('f' -> PF), ('g' -> PG))

  case class Note(octave: Octave, pitch: Pitch)

  def example[F[_]](
    eKey: Event[Char]
  )(implicit
    functorEvent: Functor[Event],
    applicativeSignal: Applicative[Signal],
    L: Frp[F]
  ): Signal[Note] = {

    // TODO: flatten
    val ePitch: Event[Pitch] = eKey.map(e => table.get(e)).filter(_.isDefined).map(_.get)

    def eOctChange(c: Char): Option[Octave => Octave] = c match {
      case '+' => Some((x: Octave) => x + 1)
      case '-' => Some((x: Octave) => x - 1)
      case _   => None
    }

    // TODO: flatten
    val bOctave: Signal[Octave] = eKey.map(eOctChange).filter(_.isDefined).map(_.get).accumS(0)
    val bPitch: Signal[Pitch]   = ePitch.stepper(PC)

    (bOctave |@| bPitch).apply(Note(_, _))

  }
}
