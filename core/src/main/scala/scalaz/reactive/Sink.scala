//package scalaz.reactive
//import scalaz.zio.IO
//
//object Sink {
//
//  type Sink[A] = A => IO[Void, Unit]
//
//  def sinkR[A, B](sink: Sink[A], r: Reactive[A]): IO[Void, Unit] =
//    sink(r.head).flatMap(_ => sinkE(sink, r.tail))
//
//  def sinkE[A, B](sink: Sink[A], e: Event[A]): IO[Void, Unit] =
//    e.value.flatMap { case (_, r) => sinkR(sink, r) }
//
//  def sinkB[A, B](b: Behaviour[A], tfSink: Sink[TimeFun[A]]): IO[Void, Unit] =
//    sinkR(tfSink, b.value)
//
//}
