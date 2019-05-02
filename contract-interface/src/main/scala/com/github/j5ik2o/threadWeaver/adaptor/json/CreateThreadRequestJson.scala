package com.github.j5ik2o.threadWeaver.adaptor.json

case class CreateThreadRequestJson(
    parentThreadId: Option[String],
    administratorIds: Seq[String],
    memberIds: Seq[String],
    createAt: Long
)
