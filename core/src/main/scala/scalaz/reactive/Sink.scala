package scalaz.reactive
import scalaz.zio.IO

/*
  sinkR :: Sink a → Reactive a → IO b
  sinkE :: Sink a → Event a → IO b

The implementation is an extremely simple back-and-forth, with
sinkR rendering initial values and sinkE waiting until the next
event occurrence.

  sinkR snk (a ‘Stepper ‘ e) = snk a >> sinkE snk e
  sinkE snk (Ev (Fut (tˆr , r ))) = waitFor tˆr >> sinkR snk r
 */
case class Sink[A, B](f: A => IO[Void, Unit]) {

  def sink(r: Reactive[A]): IO[Void, B] =
    IO.point(f(r.head)).flatMap(_ => sink(r.tail)) // how to >> ?
  def sink(e: Event[A]): IO[Void, B] = e.value.flatMap { case (_, r) => sink(r) }
}
