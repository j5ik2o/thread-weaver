package com.github.j5ik2o.threadWeaver.domain.model.threads

import cats.Semigroup
import cats.data.NonEmptyList
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId

final case class AdministratorIds(breachEncapsulationOfValues: NonEmptyList[AccountId]) {

  def contains(value: AccountId): Boolean =
    breachEncapsulationOfValues.toList.contains(value)

  def filterNot(other: AdministratorIds): Either[Exception, AdministratorIds] = {
    val list = breachEncapsulationOfValues.filterNot(p => other.contains(p))
    if (list.isEmpty)
      Left(new IllegalArgumentException("Administrators can not be empty."))
    else
      Right(AdministratorIds(list.head, list.tail: _*))
  }

  def valuesAsString: Seq[String] = breachEncapsulationOfValues.toList.map(_.value.asString)

}

object AdministratorIds {

  def apply(head: AccountId, tail: List[AccountId]): AdministratorIds =
    new AdministratorIds(NonEmptyList.of(head, tail: _*))

  def apply(head: AccountId, tail: AccountId*): AdministratorIds =
    new AdministratorIds(NonEmptyList.of(head, tail: _*))

  implicit object MessagesSemigroup extends Semigroup[AdministratorIds] {

    override def combine(x: AdministratorIds, y: AdministratorIds): AdministratorIds =
      AdministratorIds(x.breachEncapsulationOfValues ::: y.breachEncapsulationOfValues)
  }

}
