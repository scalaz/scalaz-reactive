package scalaz.reactive

import scalaz.Scalaz._
import scalaz.zio.IO

object Future {

  def merge[A](f1: Future[A], f2: Future[A]): Future[A] = f1.flatMap {
    case (t1, a1) =>
      f2.map { case (t2, a2) => if (t1 <= t2) (t1, a1) else (t2, a2) }
  }

  type Infallible[A] = IO[Nothing, A]
  type Future[+A]    = IO[Void, (Time, A)]
  def Never[A]: Future[A] = IO.never
}
