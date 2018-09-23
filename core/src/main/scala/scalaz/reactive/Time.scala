package scalaz.reactive

import scalaz.Order
import scalaz.Ordering.{ EQ, GT, LT }
import scalaz.reactive.Time.{ NegInf, PosInf, T }
import scalaz.zio.IO

sealed trait Time

object Time extends TimeInstances0 {

  case object NegInf extends Time

  case class T(value: Long) extends Time

  case object PosInf extends Time

  def now: IO[Void, T] = IO.sync(T(System.currentTimeMillis()))
}

trait TimeInstances0 {

  implicit val orderTime: Order[Time] =
    (x: Time, y: Time) =>
      (x, y) match {
        case (x0, y0) if x0 == y0      => EQ
        case (NegInf, _)               => LT
        case (_, NegInf)               => GT
        case (PosInf, _)               => GT
        case (_, PosInf)               => LT
        case (T(xt), T(yt)) if xt < yt => LT
        case (T(xt), T(yt)) if xt > yt => GT
        case (T(_), T(_))              => EQ
      }
}
