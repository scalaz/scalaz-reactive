package scalaz.reactive

import scalaz.{Monad, Monoid}

trait FutureInstances {

  implicit def monoidFuture[F[_]: Sync, A] = new Monoid[Future[F, A]] {


    lazy val noVal: F[A] = Sync[F].suspend { println("Instantiating a"); ??? }
    lazy val never: F[Time] = Monad[F].pure(Time.PosInf)

    override def zero: Future[F, A] =
      Future(never, noVal)
    override def append(f1: Future[F, A], f2: => Future[F, A]): Future[F, A] =
      ???
  }

}
