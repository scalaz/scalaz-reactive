package scalaz.reactive

import scalaz.Order
import scalaz.Ordering.{LT, EQ, GT}

sealed trait Time

object Time {

  case object NegInf extends Time

  case class T(value: Long) extends Time

  case object PosInf extends Time

  implicit val orderTime: Order[Time] =
    (x: Time, y: Time) => (x, y) match {
      case (x0, y0) if x0 == y0 => EQ
      case (NegInf, _) => LT
      case (_, NegInf) => GT
      case (PosInf, _) => GT
      case (_, PosInf) => LT
      case (T(xt), T(yt)) if xt < yt => LT
      case (T(xt), T(yt)) if xt > yt => GT
    }
}
