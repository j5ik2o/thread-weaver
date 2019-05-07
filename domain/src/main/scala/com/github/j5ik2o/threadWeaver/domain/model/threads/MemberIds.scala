package com.github.j5ik2o.threadWeaver.domain.model.threads

import cats.Monoid
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId

final case class MemberIds(breachEncapsulationOfValues: Vector[AccountId]) {

  def contains(value: AccountId): Boolean =
    breachEncapsulationOfValues.contains(value)

  def filterNot(other: MemberIds): MemberIds = {
    val list = breachEncapsulationOfValues.filterNot(p => other.contains(p))
    MemberIds(list: _*)
  }

}

object MemberIds {

  def apply(values: AccountId*) = new MemberIds(Vector(values: _*))

  val empty = MemberIds(Vector.empty)

  implicit val memberIdsMonoid: Monoid[MemberIds] = new Monoid[MemberIds] {
    override def empty: MemberIds = MemberIds.empty

    override def combine(x: MemberIds, y: MemberIds): MemberIds =
      MemberIds(x.breachEncapsulationOfValues ++ y.breachEncapsulationOfValues)
  }

}
