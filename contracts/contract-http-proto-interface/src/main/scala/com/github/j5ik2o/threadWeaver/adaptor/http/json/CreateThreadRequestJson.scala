package com.github.j5ik2o.threadWeaver.adaptor.http.json

final case class CreateThreadRequestJson(
    accountId: String,
    parentThreadId: Option[String],
    title: String,
    remarks: Option[String],
    administratorIds: Seq[String],
    memberIds: Seq[String],
    createAt: Long
)
