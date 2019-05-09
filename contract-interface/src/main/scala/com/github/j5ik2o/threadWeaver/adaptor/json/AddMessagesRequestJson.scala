package com.github.j5ik2o.threadWeaver.adaptor.json

final case class TextMessage(replyMessageId: Option[String], toAccountIds: Seq[String], text: String)

final case class AddMessagesRequestJson(senderId: String, messages: Seq[TextMessage], createAt: Long)
