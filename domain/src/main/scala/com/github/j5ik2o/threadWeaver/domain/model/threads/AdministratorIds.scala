package com.github.j5ik2o.threadWeaver.domain.model.threads

import cats.Semigroup
import cats.data.NonEmptyList
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId

final case class AdministratorIds(breachEncapsulationOfValues: NonEmptyList[AccountId]) {

  def contains(value: AccountId): Boolean =
    breachEncapsulationOfValues.toList.contains(value)
}

object AdministratorIds {

  def apply(head: AccountId, tail: AccountId*): AdministratorIds =
    new AdministratorIds(NonEmptyList.of(head, tail: _*))

  implicit object MessagesSemigroup extends Semigroup[AdministratorIds] {

    override def combine(x: AdministratorIds, y: AdministratorIds): AdministratorIds =
      AdministratorIds(x.breachEncapsulationOfValues ::: y.breachEncapsulationOfValues)
  }

}
