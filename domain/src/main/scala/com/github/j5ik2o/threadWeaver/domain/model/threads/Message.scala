package com.github.j5ik2o.threadWeaver.domain.model.threads

import java.time.Instant

import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId

sealed trait Message[A] {
  def id: MessageId

  def `type`: String

  def body: A

  def senderId: AccountId

  def createdAt: Instant

  def updatedAt: Instant
}

final case class TextMessage(
    id: MessageId,
    replyMessageId: Option[MessageId],
    toAccountIds: ToAccountIds,
    body: Text,
    senderId: AccountId,
    createdAt: Instant,
    updatedAt: Instant
) extends Message[Text] {
  override def `type`: String = "text"
}
