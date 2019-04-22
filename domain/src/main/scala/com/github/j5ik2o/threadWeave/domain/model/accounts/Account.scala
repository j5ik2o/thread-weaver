package com.github.j5ik2o.threadWeave.domain.model.accounts

import java.time.Instant

import com.github.j5ik2o.threadWeave.infrastructure.ulid.ULID
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.string.MatchesRegex

final case class AccountId(value: ULID)

object AccountName {

  type ValueType =
    String Refined And[MatchesRegex[W.`"[a-z][a-zA-Z0-9]+"`.T], Size[Interval.Closed[W.`1`.T, W.`255`.T]]]
}

final case class AccountName(name: AccountName.ValueType)

final case class Account(id: AccountId, name: AccountName, createdAt: Instant, updatedAt: Instant)
