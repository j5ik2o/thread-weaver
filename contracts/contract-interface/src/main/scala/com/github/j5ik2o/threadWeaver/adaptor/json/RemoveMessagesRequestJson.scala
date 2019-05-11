package com.github.j5ik2o.threadWeaver.adaptor.json

final case class RemoveMessagesRequestJson(senderId: String, messageIds: Seq[String], createAt: Long)
