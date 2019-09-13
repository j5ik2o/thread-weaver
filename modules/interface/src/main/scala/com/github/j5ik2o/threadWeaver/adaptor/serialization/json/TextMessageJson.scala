package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

case class TextMessageJson(
    id: String,
    replyMessageId: Option[String],
    toAccountIds: Seq[String],
    `type`: String,
    body: String,
    senderId: String,
    createdAt: Long,
    updatedAt: Long
)
