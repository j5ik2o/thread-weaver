package com.github.j5ik2o.threadWeaver.adaptor.json

final case class ThreadMessageJson(id: String,
                                   threadId: String,
                                   senderId: String,
                                   `type`: String,
                                   body: String,
                                   createdAt: Long,
                                   updatedAt: Long)

final case class GetThreadMessagesResponseJson(result: Seq[ThreadMessageJson], error_messages: Seq[String] = Seq.empty)
    extends ResponseJson
