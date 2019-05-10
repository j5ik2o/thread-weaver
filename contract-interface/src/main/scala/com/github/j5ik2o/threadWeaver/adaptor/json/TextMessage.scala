package com.github.j5ik2o.threadWeaver.adaptor.json

final case class TextMessage(replyMessageId: Option[String], toAccountIds: Seq[String], text: String)
