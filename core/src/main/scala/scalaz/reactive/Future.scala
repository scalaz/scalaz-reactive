package scalaz.reactive

import scalaz._
import scalaz.Scalaz._

trait FutureInstances {

  val monEither = implicitly[Functor[Either[Int, ?]]]
  val monOpt: Functor[Option] = implicitly[Functor[Option]]

  def mapFuture[F[_]: Sync, ?] = new Functor[Future[F, ?]] {
    override def map[A, B](fa: Future[F, A])(f: A => B): Future[F, B] =
      Future(fa.ftv.map(tv => TimedValue(tv.t, _ => f(tv.get))))

  }

}
