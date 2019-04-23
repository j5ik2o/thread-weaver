package com.github.j5ik2o.threadWeave.domain.model.threads

import java.time.Instant

import cats.implicits._
import cats.{ Monoid, Semigroup }
import cats.data.NonEmptyList
import com.github.j5ik2o.threadWeave.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeave.infrastructure.ulid.ULID

final case class ThreadId(value: ULID = ULID())

final case class AdministratorIds(breachEncapsulationOfValues: NonEmptyList[AccountId]) {

  def contains(value: AccountId): Boolean =
    breachEncapsulationOfValues.toList.contains(value)
}

object AdministratorIds {

  def apply(head: AccountId, tail: AccountId*): AdministratorIds =
    new AdministratorIds(NonEmptyList.of(head, tail: _*))

  implicit object MessagesSemigroup extends Semigroup[AdministratorIds] {

    override def combine(x: AdministratorIds, y: AdministratorIds): AdministratorIds =
      AdministratorIds(x.breachEncapsulationOfValues ::: y.breachEncapsulationOfValues)
  }

}

final case class MemberIds(breachEncapsulationOfValues: Vector[AccountId]) {

  def contains(value: AccountId): Boolean =
    breachEncapsulationOfValues.contains(value)
}

object MemberIds {

  def apply(values: AccountId*) = new MemberIds(Vector(values: _*))

  val empty = MemberIds(Vector.empty)

  implicit val memberIdsMonoid: Monoid[MemberIds] = new Monoid[MemberIds] {
    override def empty: MemberIds = MemberIds.empty

    override def combine(x: MemberIds, y: MemberIds): MemberIds =
      MemberIds(x.breachEncapsulationOfValues ++ y.breachEncapsulationOfValues)
  }
}

final case class MessageId(value: ULID = ULID())
final case class Text(value: String)
final case class ToAccountIds(values: Vector[AccountId])

object ToAccountIds {
  val empty = ToAccountIds(Vector.empty)
}
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

final case class MessageIds(breachEncapsulationOfValues: Vector[MessageId]) {

  def contains(value: MessageId): Boolean =
    breachEncapsulationOfValues.contains(value)
}

final case class Messages(breachEncapsulationOfValues: Vector[Message[_]]) {

  def filterNot(messageIds: MessageIds, senderId: AccountId): Messages = {
    copy(
      breachEncapsulationOfValues =
        breachEncapsulationOfValues.filterNot(v => v.senderId == senderId && messageIds.contains(v.id))
    )
  }

}

object Messages {

  def apply(values: Message[_]*): Messages = new Messages(Vector(values: _*))

  val empty = Messages(Vector.empty)

  implicit val messagesMonoid: Monoid[Messages] = new Monoid[Messages] {
    override def empty: Messages = Messages.empty

    override def combine(x: Messages, y: Messages): Messages =
      Messages(x.breachEncapsulationOfValues ++ y.breachEncapsulationOfValues)
  }
}

final case class Thread(
    id: ThreadId,
    parentThreadId: Option[ThreadId],
    administratorIds: AdministratorIds,
    memberIds: MemberIds,
    messages: Messages,
    createdAt: Instant,
    updatedAt: Instant
) {

  def isAdministratorId(accountId: AccountId): Boolean =
    administratorIds.contains(accountId)

  def isMemberId(accountId: AccountId): Boolean =
    memberIds.contains(accountId)

  def addAdministratorIds(value: AdministratorIds, senderId: AccountId): Either[Exception, Thread] = {
    if (isAdministratorId(senderId))
      Right(copy(administratorIds = administratorIds combine value))
    else
      Left(new Exception("senderId is not administrator"))
  }

  def addMemberIds(value: MemberIds, senderId: AccountId): Either[Exception, Thread] = {
    if (isAdministratorId(senderId))
      Right(copy(memberIds = memberIds combine value))
    else
      Left(new Exception("senderId is not administrator."))
  }

  def addMessages(values: Messages, at: Instant): Either[Exception, Thread] = {
    if (values.breachEncapsulationOfValues.exists(v => isMemberId(v.senderId)))
      Right(copy(messages = messages combine values, updatedAt = at))
    else
      Left(new Exception("senderId is not member"))
  }

  def filterNotMessages(values: MessageIds, senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isAdministratorId(senderId))
      Right(copy(messages = messages.filterNot(values, senderId)))
    else
      Left(new Exception())
  }

}
