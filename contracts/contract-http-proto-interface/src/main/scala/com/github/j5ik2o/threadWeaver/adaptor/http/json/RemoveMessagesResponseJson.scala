package com.github.j5ik2o.threadWeaver.adaptor.http.json

final case class RemoveMessagesResponseJson(messageIds: Seq[String], error_messages: Seq[String] = Seq.empty)
    extends ResponseJson
