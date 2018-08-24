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

object Sink {

  type Sink[A] = A => IO[Void, Unit]

  def sinkR[A, B](sink: Sink[A], r: Reactive[A]): IO[Void, Unit] =
    IO.sync(sink(r.head)).flatMap(_ => sinkE(sink, r.tail)) // how to >> ?
  def sinkE[A, B](sink: Sink[A], e: Event[A]): IO[Void, Unit] =
    e.value.flatMap {
      case (_, r) => sinkR(sink, r)
    }
//  sinkB :: Sink a → Behavior a → IO b
//    sinkB snk (O rf ) = do
//        snkF ← newTFunSink snk
//        sinkR snkF rf
  def sinkB[A, B](sink: Sink[A], b: Behaviour[A], fs: Sink[A] => Sink[TimeFun[A]]): IO[Void, Unit] = {
    sinkR(fs(sink), b.value)
  }


}
