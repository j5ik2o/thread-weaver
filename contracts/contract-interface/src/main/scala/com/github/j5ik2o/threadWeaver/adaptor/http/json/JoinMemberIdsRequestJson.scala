package com.github.j5ik2o.threadWeaver.adaptor.http.json

final case class JoinMemberIdsRequestJson(accountId: String, accountIds: Seq[String], createAt: Long)
