package com.github.j5ik2o.threadWeaver.adaptor.http.json

final case class ThreadMessageJson(
    id: String,
    threadId: String,
    senderId: String,
    `type`: String,
    body: String,
    createdAt: Long,
    updatedAt: Long
)
