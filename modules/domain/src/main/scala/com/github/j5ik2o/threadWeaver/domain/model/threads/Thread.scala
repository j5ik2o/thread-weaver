package com.github.j5ik2o.threadWeaver.domain.model.threads

import java.time.Instant

import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import cats.implicits._
import com.github.j5ik2o.threadWeaver.domain.model.threads.Thread.Result

object Thread {
  type Result[A] = Either[Exception, A]
  val MessagesLimit = 5000

  def apply(
      id: ThreadId,
      creatorId: AccountId,
      parentThreadId: Option[ThreadId],
      title: ThreadTitle,
      remarks: Option[ThreadRemarks],
      administratorIds: AdministratorIds,
      memberIds: MemberIds,
      messages: Messages,
      createdAt: Instant,
      updatedAt: Instant,
      removedAt: Option[Instant] = None
  ): Thread =
    ThreadImpl(
      id,
      creatorId,
      parentThreadId,
      title,
      remarks,
      administratorIds,
      memberIds,
      messages,
      createdAt,
      updatedAt,
      removedAt
    )
}

trait Thread {

  def isAdministratorId(accountId: AccountId): Boolean
  def isMemberId(accountId: AccountId): Boolean

  def joinAdministratorIds(value: AdministratorIds, senderId: AccountId, at: Instant): Result[Thread]
  def leaveAdministratorIds(value: AdministratorIds, senderId: AccountId, at: Instant): Result[Thread]
  def getAdministratorIds(senderId: AccountId): Result[AdministratorIds]

  def joinMemberIds(value: MemberIds, senderId: AccountId, at: Instant): Result[Thread]
  def leaveMemberIds(value: MemberIds, senderId: AccountId, at: Instant): Result[Thread]
  def getMemberIds(senderId: AccountId): Result[MemberIds]

  def addMessages(values: Messages, at: Instant): Result[Thread]
  def removeMessages(values: MessageIds, removerId: AccountId, at: Instant): Result[(Thread, MessageIds)]
  def getMessages(senderId: AccountId): Result[Messages]

  def destroy(senderId: AccountId, at: Instant): Result[Thread]
}

final case class ThreadImpl(
    id: ThreadId,
    creatorId: AccountId,
    parentThreadId: Option[ThreadId],
    title: ThreadTitle,
    remarks: Option[ThreadRemarks],
    private val administratorIds: AdministratorIds,
    private val memberIds: MemberIds,
    private val messages: Messages,
    createdAt: Instant,
    updatedAt: Instant,
    removedAt: Option[Instant] = None
) extends Thread {

  private def isRemoved: Boolean =
    removedAt.fold(false) { rat =>
      Instant.now.isAfter(rat)
    }

  override def isAdministratorId(accountId: AccountId): Boolean =
    administratorIds.contains(accountId)

  override def isMemberId(accountId: AccountId): Boolean =
    memberIds.contains(accountId) || administratorIds.contains(accountId)

  override def joinAdministratorIds(
      value: AdministratorIds,
      senderId: AccountId,
      at: Instant
  ): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed thread"))
    else if (!isAdministratorId(senderId))
      Left(new IllegalArgumentException("senderId is not administrator"))
    else
      Right(copy(administratorIds = administratorIds combine value, updatedAt = at))
  }

  override def leaveAdministratorIds(
      value: AdministratorIds,
      senderId: AccountId,
      at: Instant
  ): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed thread"))
    else if (!isAdministratorId(senderId))
      Left(new IllegalArgumentException("senderId is not administrator"))
    else
      administratorIds.filterNot(value).map { result =>
        copy(administratorIds = result, updatedAt = at)
      }
  }

  override def getAdministratorIds(senderId: AccountId): Either[Exception, AdministratorIds] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isMemberId(senderId))
      Left(new IllegalArgumentException("The senderId is not the administrator"))
    else
      Right(administratorIds)
  }

  override def joinMemberIds(value: MemberIds, senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isAdministratorId(senderId))
      Left(new IllegalArgumentException("the senderId is not the administrator"))
    else
      Right(copy(memberIds = memberIds combine value, updatedAt = at))
  }

  override def leaveMemberIds(value: MemberIds, senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isAdministratorId(senderId))
      Left(new IllegalArgumentException("the senderId is not the administrator"))
    else
      Right(copy(memberIds = memberIds.filterNot(value), updatedAt = at))
  }

  override def getMemberIds(senderId: AccountId): Either[Exception, MemberIds] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isMemberId(senderId))
      Left(new IllegalArgumentException("the senderId is not the administrator"))
    else
      Right(memberIds)
  }

  override def addMessages(values: Messages, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!values.forall(v => isMemberId(v.senderId)))
      Left(new IllegalArgumentException("The senderId is not the member"))
    else if (messages.size + values.size > Thread.MessagesLimit)
      Left(new IllegalArgumentException("The limit of messages size is over"))
    else
      Right(copy(messages = messages combine values, updatedAt = at))
  }

  override def removeMessages(
      values: MessageIds,
      removerId: AccountId,
      at: Instant
  ): Either[Exception, (Thread, MessageIds)] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isMemberId(removerId))
      Left(new IllegalArgumentException("The removerId is not the member"))
    else {
      if (!messages.filter(values).forall(_.senderId == removerId))
        Left(new IllegalArgumentException("The removerId is not the senderId"))
      else {
        val newMessage = messages.filterNot(values)
        val diff       = messages.toMessageIds.diff(newMessage.toMessageIds)
        Right((copy(messages = newMessage, updatedAt = at), diff))
      }
    }
  }

  override def getMessages(senderId: AccountId): Either[Exception, Messages] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isMemberId(senderId))
      Left(new IllegalArgumentException("The senderId is not the member"))
    else
      Right(messages)
  }

  override def destroy(senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isAdministratorId(senderId))
      Left(new IllegalArgumentException("The senderId is not the administrator"))
    else
      Right(copy(removedAt = Some(at)))
  }

}
