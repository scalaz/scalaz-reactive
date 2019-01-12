package scalaz.reactive.examples

import scalaz.{Monad, Monoid}
import scalaz.Scalaz._
import scalaz.reactive.io.types.IO1
import scalaz.reactive._
import scalaz.reactive.io.FrpIo
import scalaz.zio.{App, IO}

import scala.concurrent.duration.{Duration, _}
import scala.language.postfixOps

object TwoTickers1 extends App with FutureInstances {

  import scalaz.reactive.Ops._
  import scalaz.reactive.io.instances._

  case class Tick(name: String)

  implicit val frp = FrpIo

  implicit val m: Monad[IO1] = monadIo

  class Program[F[_]](implicit val frp: Frp[F], m: Sync[F], monoFuture: Monoid[Future[F, Tick]]) {

    def ticks(interval: Duration, name: String): F[Event[F, Tick]] = {

      def tail(): F[Event[F, Tick]] =
        ticks(interval, name).flatMap(tss => frp.delay(tss, interval))

      val reactive: F[Reactive[F, Tick]] = for {
        head <- m.point(Tick(name))
        tail <- tail()
      } yield Reactive(head, tail)

      reactive.map { r =>
        Event(Future {
          frp.now.map(t => TimedValue(Improving.exactly(t), _ => r))
        })
      }
    }

    def myAppLogic: F[Unit] = {
      val sink: Tick => F[Unit] = (t: Tick) =>
        frp.pure(println(s"tick ${t.name}"))

      val eventA: F[Event[F, Tick]] = ticks(1.2 second, "a")
      eventA.map( e => new Ops.eventOps(e))
      val eventB: F[Event[F, Tick]] = ticks(2.1 second, "b")
      val merged: F[Event[F, Tick]] =
        m.bind(eventA)(a => m.bind(eventB)(b => a.merge(b)))

      merged.flatMap(frp.sinkE(sink, _))
    }
  }

  override def run(args: List[String]): IO[Nothing, TwoTickers1.ExitStatus] = {
    val logic: IO1[Unit] = new Program().myAppLogic
    val attempted: IO[Nothing, Either[Void, Unit]] = logic.attempt
    attempted.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))
  }
}
