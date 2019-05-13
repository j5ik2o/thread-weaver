package com.github.j5ik2o.threadWeaver.adaptor.http.json

final case class LeaveMemberIdsRequestJson(removerId: String, accountIds: Seq[String], createAt: Long)
