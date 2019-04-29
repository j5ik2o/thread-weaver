package com.github.j5ik2o.threadWeaver.domain.model.threads

import cats.Monoid
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId

final case class ToAccountIds(breachEncapsulationOfValues: Vector[AccountId])

object ToAccountIds {

  def apply(values: AccountId*): ToAccountIds = new ToAccountIds(Vector(values: _*))

  val empty = ToAccountIds(Vector.empty)

  implicit val toAccountIdsMonoid: Monoid[ToAccountIds] = new Monoid[ToAccountIds] {
    override def empty: ToAccountIds = ToAccountIds.empty

    override def combine(x: ToAccountIds, y: ToAccountIds): ToAccountIds =
      ToAccountIds(x.breachEncapsulationOfValues ++ y.breachEncapsulationOfValues)
  }

}
