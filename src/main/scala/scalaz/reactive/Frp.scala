package scalaz.reactive


/**
  * Stream of events
  */
trait Event[A]

/**
  * Value changin in time, aka behavior
  */


trait Signal[A]

trait Frp[F[_]] {

  type Time = Int

  def stepper[A](initial: A, event: Event[A]): Signal[A]

  def accumS[A](initial: A, event: Event[A => A]): Signal[A]

  def signal[A](f: Time => A): Signal[A]

  def filterE[A](event: Event[A], f: A => Boolean): Event[A]

}


object Ops {

  implicit class EventOps[F[_], A](e: Event[A])(implicit L: Frp[F]) {
    def filter(f: A => Boolean) = L.filterE(e, f)

    def stepper(initial: A) = L.stepper(initial, e)
  }

  implicit class EventOps1[F[_], A](e: Event[A => A])(implicit L: Frp[F]) {
    def accumS(initial: A) = L.accumS(initial, e)
  }


}


