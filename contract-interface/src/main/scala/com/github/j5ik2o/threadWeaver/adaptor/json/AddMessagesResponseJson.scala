package com.github.j5ik2o.threadWeaver.adaptor.json

final case class AddMessagesResponseJson(id: Option[String], error_messages: Seq[String] = Seq.empty)
    extends ResponseJson