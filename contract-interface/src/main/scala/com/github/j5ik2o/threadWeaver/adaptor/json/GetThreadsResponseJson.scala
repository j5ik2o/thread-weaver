package com.github.j5ik2o.threadWeaver.adaptor.json

final case class ThreadJson(
    id: String,
    creatorId: String,
    parentThreadId: Option[String],
    title: String,
    remarks: Option[String],
    createdAt: Long,
    updatedAt: Long
)

final case class GetThreadsResponseJson(results: Seq[ThreadJson], error_messages: Seq[String] = Seq.empty)
    extends ResponseJson
