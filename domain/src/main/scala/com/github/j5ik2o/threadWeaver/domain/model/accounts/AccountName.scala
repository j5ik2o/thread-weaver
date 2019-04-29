package com.github.j5ik2o.threadWeaver.domain.model.accounts

import cats.implicits._
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountName.{ DomainError, FormatError }
import eu.timepit.refined.W
import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.string.MatchesRegex

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
