package com.github.j5ik2o.threadWeaver.adaptor.http.json

final case class ThreadJson(
    id: String,
    creatorId: String,
    parentThreadId: Option[String],
    title: String,
    remarks: Option[String],
    createdAt: Long,
    updatedAt: Long
)
