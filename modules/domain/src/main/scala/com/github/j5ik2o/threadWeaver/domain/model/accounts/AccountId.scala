package com.github.j5ik2o.threadWeaver.domain.model.accounts

import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

final case class AccountId(value: ULID = ULID()) {
  def asString: String = value.asString
}
