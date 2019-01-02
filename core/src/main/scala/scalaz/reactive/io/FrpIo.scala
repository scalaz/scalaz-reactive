package scalaz.reactive.io

import scalaz._
import scalaz.reactive.Time._
import scalaz.reactive._
import scalaz.reactive.io.types.IO1
import scalaz.zio.IO

import scala.concurrent.duration.Duration

package object types {

  type IO1[A] = IO[Void, A]
}

object FrpIo extends Frp[IO1] {

  override def merge[A](f1: => Future[IO1, A],
                        f2: Future[IO1, A]): Future[IO1, A] = ???

  override def merge[A](a: => Event[IO1, A],
                        b: Event[IO1, A]): IO1[Event[IO1, A]] = ???

  def delay[A](e: => Event[IO1, A], interval: Duration): IO1[Event[IO1, A]] =
    IO.sync(e).delay(interval)

  override def now: IO1[Time] = IO.sync(T(System.currentTimeMillis()))

  override def pure[A](a: => A): IO1[A] = IO.now(a)

  override def sinkR[A](sink: Sink[A], r: Reactive[IO1, A]): IO1[Unit] =
    sink(r.head).flatMap(_ => sinkE(sink, r.tail))

  override def sinkE[A](sink: Sink[A], e: Event[IO1, A]): IO1[Unit] = {
    e.value.v.flatMap(r => sinkR(sink, r))
  }

  override def never[A]: Event[IO1, A] = ???
}

object instances {

  implicit object monadIo extends Monad[IO1] {
    override def point[A](a: => A): IO1[A] = { IO.point(a) }

    override def bind[A, B](fa: IO1[A])(f: A => IO1[B]): IO1[B] = fa.flatMap(f)

    override def map[A, B](fa: IO1[A])(f: A => B): IO1[B] = fa.map(f)

  }

  implicit object syncIo extends Sync[IO1] {
    override def suspend[A](thunk: => IO1[A]): IO1[A] = thunk

    override def bind[A, B](fa: IO1[A])(f: A => IO1[B]): IO1[B] =
      monadIo.bind(fa)(f)
    override def point[A](a: => A): IO1[A] = monadIo.point(a)
  }

}
