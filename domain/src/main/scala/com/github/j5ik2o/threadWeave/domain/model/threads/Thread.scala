package com.github.j5ik2o.threadWeave.domain.model.threads

import java.time.Instant

import com.github.j5ik2o.threadWeave.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeave.infrastructure.ulid.ULID

final case class ThreadId(value: ULID)
final case class AdminAccountIds(values: Vector[AccountId])
final case class MemberAccountIds(values: Vector[AccountId])
final case class MessageId(value: ULID)
final case class Text(value: String)
sealed trait Message[B] {
  def id: MessageId
  def `type`: String
  def body: B
  def createdAt: Instant
  def updatedAt: Instant
}
final case class ToAccountIds(values: Vector[AccountId])
final case class TextMessage(
    id: MessageId,
    replyMessageId: Option[MessageId],
    toAccountIds: ToAccountIds,
    body: Text,
    createdAt: Instant,
    updatedAt: Instant
) extends Message[Text] {
  override def `type`: String = "text"
}
final case class Messages(values: Vector[Message[_]])
final case class Thread(
    id: ThreadId,
    adminAccountIds: AdminAccountIds,
    memberAccountIds: MemberAccountIds,
    messages: Messages,
    createdAt: Instant
)
