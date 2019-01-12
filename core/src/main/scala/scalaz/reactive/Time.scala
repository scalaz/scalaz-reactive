package scalaz.reactive

import java.text.SimpleDateFormat

import scalaz._
import Scalaz._
import scalaz.Ordering.{EQ, GT, LT}
import scalaz.reactive.Time.{After, NegInf, PosInf, T}

sealed trait Time

object Time extends TimeInstances0 {

  case object NegInf extends Time

  case class After(value: Long) extends Time

  case class T(value: Long) extends Time

  case object PosInf extends Time

  def now = T(System.currentTimeMillis)
}


trait TimeInstances0 {

  val DateFormat = "HH:mm:ss.SSS"

  implicit val showTime: Show[Time] = new Show[Time] {
    override def shows(t: Time): String = t match {
      case NegInf => "-Inf"
      case PosInf => "+Inf"
      case After(t) => s"After(${new SimpleDateFormat(DateFormat).format(t)}"
      case T(t) => s"T(${new SimpleDateFormat(DateFormat).format(t)}"
    }
  }

  implicit val orderTime: Order[Time] =
    (x: Time, y: Time) => {
      val result = (x, y) match {
        case (x0, y0) if x0 == y0      => EQ
        case (NegInf, _)               => LT
        case (_, NegInf)               => GT
        case (PosInf, _)               => GT
        case (_, PosInf)               => LT
        case (T(xt), T(yt)) if xt < yt => LT
        case (T(xt), T(yt)) if xt > yt => GT
        case (T(_), T(_))              => EQ
        case (After(_), T(_))          => LT
        case (T(_), After(_))          => GT
      }
      println (s"Compared [${x.shows}, ${y.shows}]: $result")
      result
    }
}
