package com.github.j5ik2o.threadWeaver.adaptor.json

final case class GetThreadMessagesResponseJson(result: Seq[ThreadMessageJson], error_messages: Seq[String] = Seq.empty)
    extends ResponseJson
