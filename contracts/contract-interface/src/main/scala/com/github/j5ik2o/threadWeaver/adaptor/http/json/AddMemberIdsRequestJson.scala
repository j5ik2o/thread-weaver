package com.github.j5ik2o.threadWeaver.adaptor.http.json

final case class AddMemberIdsRequestJson(adderId: String, memberIds: Seq[String], createAt: Long)
