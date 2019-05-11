package com.github.j5ik2o.threadWeaver.domain.model.accounts

import java.time.Instant

final case class Account(id: AccountId, name: AccountName, createdAt: Instant, updatedAt: Instant) {

  def withName(value: AccountName): Account = copy(name = value)

  def withUpdatedAt(value: Instant): Account = copy(updatedAt = value)

}
