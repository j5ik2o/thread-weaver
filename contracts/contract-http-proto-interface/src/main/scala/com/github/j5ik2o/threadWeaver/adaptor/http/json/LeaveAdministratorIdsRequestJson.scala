package com.github.j5ik2o.threadWeaver.adaptor.http.json

final case class LeaveAdministratorIdsRequestJson(accountId: String, administratorIds: Seq[String], createAt: Long)
