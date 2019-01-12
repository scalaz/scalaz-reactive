package scalaz.reactive

import scalaz._
import Scalaz._
import scalaz.Ordering._

trait Improving[A] {
  def exact: A
  def compare(other: A)(implicit ord: Order[A]): scalaz.Ordering
}

case class FTime(ft: Improving[Time])

object Improving {

  def exactly[A](a: A) =
    new Improving[A] {
      override def exact: A = a
      override def compare(other: A)(implicit ord: Order[A]): Ordering =
        Order[A].order(a, other)
    }

  def afterNow: Improving[Time] =
    new Improving[Time] {
      override def exact: Time = Time.now
      override def compare(other: Time)(implicit ord: Order[Time]): Ordering =
        Order[Time].order(exact, other)
    }

  def now: Improving[Time] =
    new Improving[Time] {
      override def exact: Time = Time.now
      override def compare(other: Time)(implicit ord: Order[Time]): Ordering =
        Order[Time].order(exact, other)
    }

  def unamb[F[_]: Sync, A](a: F[A], b: F[A]): F[A] =
    Sync[F].race(a, b).map(r => r._2)

  def minLe[F[_]: Sync, A: Order](
    futImpU: F[Improving[A]],
    futImpV: F[Improving[A]],
    afterNow: Improving[A]
  ): F[(Improving[A], Boolean)] = {

    // uLeqV = (uComp v !≡ GT) ‘unamb‘ (vComp u !≡ LT)
    lazy val uLeqV: F[Boolean] = {
      val uFirst1: F[Boolean] = for {
        impU <- futImpU
        r <- Future
          .forceF(futImpV, afterNow)
          .map(impV => impU.compare(impV.exact))
      } yield r != GT

      val uFirst2: F[Boolean] = for {
        impV <- futImpV
        r <- Future
          .forceF(futImpU, afterNow)
          .map(impU => impU.compare(impV.exact))
      } yield r != GT

      println("Running unamb")
      unamb(uFirst1, uFirst2)
    }

    lazy val uMinV: F[A] =
      uLeqV.flatMap(v => {
        val result = if (v) futImpU.map(_.exact) else futImpV.map(_.exact)
        println(s"computing uMinV from uLeqV=$v ")
        result
      })

    lazy val minComp: F[A => Ordering] = uLeqV.flatMap(
      uIsFirst =>
        if (uIsFirst)
          futImpU.map(impU => impU.compare(_))
        else
          futImpV.map(impV => impV.compare(_))
    )

//    def asAgree(a: F[Ordering], b: F[Ordering]): F[Ordering] =
//      for {
//        oa <- a
//        ob <- b
//        r <- if (oa == ob) a else Sync[F].halt[Ordering]
//      } yield r

    // wComp t = minComp t ‘unamb‘ (uComp t ‘asAgree‘ vComp t)
    lazy val comp: F[A => Ordering] = {
      val minCompT: F[A => Ordering] = minComp
//      val uCompT: F[A => Ordering] = futImpU.map(i => i.compare(_))
//      val vCompT: F[A => Ordering] = futImpV.map(i => i.compare(_))

      val combined: F[A => Ordering] = {
//        minCompT.flatMap(f => {
//          val xx: F[A => Ordering] = uCompT.map(fu => ((a: A) => fu(a)))
//          val combCombined: F[A => Ordering] = ???
//          uCompT.map(f => (a => f(a)))
//        })
//        val zz: F[A => Ordering] = (uCompT |@| vCompT) { (uc, vc) => uc }
//        (minCompT |@| zz) { (uc: A => Ordering, vc: A => Ordering) => uc
        minCompT
      }

      combined
    }

    (uLeqV |@| uMinV |@| comp) { (uLeqV: Boolean, uMinV: A, comp) =>
      val improving: Improving[A] = new Improving[A] {
        override def exact: A = uMinV
        override def compare(t: A)(implicit ord: Order[A]): scalaz.Ordering =
          comp(t)
      }
      println(s"Constucted minLe response from uLeqV=$uLeqV, uMinV=$uMinV")
      (improving, uLeqV)
    }
  }

}
