package com.github.j5ik2o.threadWeaver.adaptor.json

case class CreateThreadRequestJson(
    creatorId: String,
    parentThreadId: Option[String],
    administratorIds: Seq[String],
    memberIds: Seq[String],
    createAt: Long
)
