package com.github.j5ik2o.threadWeave.domain.model.accounts

import java.time.Instant

import com.github.j5ik2o.threadWeave.infrastructure.ulid.ULID
import eu.timepit.refined._
import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.string.MatchesRegex
import cats.implicits._
import com.github.j5ik2o.threadWeave.domain.model.accounts.AccountName.{ DomainError, FormatError }

final case class AccountId(value: ULID = ULID())

object AccountName {

  type AsString =
    String Refined And[MatchesRegex[W.`"[a-z][a-zA-Z0-9]+"`.T], Size[Interval.Closed[W.`1`.T, W.`255`.T]]]

  sealed trait DomainError
  final case class FormatError(message: String) extends DomainError

}

final case class AccountName(breachEncapsulationOfValue: AccountName.AsString) {

  def withSuffix(suffix: String): Either[DomainError, AccountName] = {
    applyRef[AccountName.AsString](breachEncapsulationOfValue + suffix)
      .leftMap[DomainError](FormatError).map(new AccountName(_))
  }

}

final case class Account(id: AccountId, name: AccountName, createdAt: Instant, updatedAt: Instant) {

  def withName(value: AccountName): Account = copy(name = value)

  def withUpdatedAt(value: Instant): Account = copy(updatedAt = value)

}
