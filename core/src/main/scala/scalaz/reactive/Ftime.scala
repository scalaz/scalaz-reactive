package scalaz.reactive

import scalaz.Ordering.{GT, LT}
import scalaz.{Order, Ordering}

case class Improving[A](exact: () => A, compare: A => Ordering)

object Improving {

  def exactly[A: Order](a: A) =
    new Improving[A](() => a, (other: A) => Order[A].order(a, other))

  def unamb[A](a: => A, b: => A): A = ???

  def minLe[A: Order](a: Improving[A],
                      b: Improving[A]): (Improving[A], Boolean) =
    (a, b) match {
      case (Improving(u, uComp), Improving(v, vComp)) =>
        val uLeqV = unamb(uComp(v()) != GT, vComp(u()) != LT)
        val uMinV = if (uLeqV) u() else v()
        val minComp = if (uLeqV) uComp else vComp
        def wComp(t: A) = unamb(minComp(t), uComp(t)) //‘asAgree‘ vComp(t))
        (Improving(() => uMinV, wComp), uLeqV)
    }

}
