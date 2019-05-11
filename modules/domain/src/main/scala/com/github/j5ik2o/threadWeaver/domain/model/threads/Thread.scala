package com.github.j5ik2o.threadWeaver.domain.model.threads

import java.time.Instant

import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import cats.implicits._

object Thread {
  val MessagesLimit = 5000
}

final case class Thread(
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
) {

  private def isRemoved: Boolean =
    removedAt.fold(false) { rat =>
      Instant.now.isAfter(rat)
    }

  def isAdministratorId(accountId: AccountId): Boolean =
    administratorIds.contains(accountId)

  def isMemberId(accountId: AccountId): Boolean =
    memberIds.contains(accountId) || administratorIds.contains(accountId)

  def joinAdministratorIds(value: AdministratorIds, senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed thread"))
    else if (!isAdministratorId(senderId))
      Left(new IllegalArgumentException("senderId is not administrator"))
    else
      Right(copy(administratorIds = administratorIds combine value, updatedAt = at))
  }

  def leaveAdministratorIds(value: AdministratorIds, senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed thread"))
    else if (!isAdministratorId(senderId))
      Left(new IllegalArgumentException("senderId is not administrator"))
    else
      administratorIds.filterNot(value).map { result =>
        copy(administratorIds = result, updatedAt = at)
      }
  }

  def getAdministratorIds(senderId: AccountId): Either[Exception, AdministratorIds] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isMemberId(senderId))
      Left(new IllegalArgumentException("The senderId is not the administrator"))
    else
      Right(administratorIds)
  }

  def joinMemberIds(value: MemberIds, senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isAdministratorId(senderId))
      Left(new IllegalArgumentException("the senderId is not the administrator"))
    else
      Right(copy(memberIds = memberIds combine value, updatedAt = at))
  }

  def leaveMemberIds(value: MemberIds, senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isAdministratorId(senderId))
      Left(new IllegalArgumentException("the senderId is not the administrator"))
    else
      Right(copy(memberIds = memberIds.filterNot(value), updatedAt = at))
  }

  def getMemberIds(senderId: AccountId): Either[Exception, MemberIds] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isMemberId(senderId))
      Left(new IllegalArgumentException("the senderId is not the administrator"))
    else
      Right(memberIds)
  }

  def addMessages(values: Messages, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!values.forall(v => isMemberId(v.senderId)))
      Left(new IllegalArgumentException("The senderId is not the member"))
    else if (messages.size + values.size > Thread.MessagesLimit)
      Left(new IllegalArgumentException("The limit of messages size is over"))
    else
      Right(copy(messages = messages combine values, updatedAt = at))
  }

  def removeMessages(values: MessageIds, removerId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isMemberId(removerId))
      Left(new IllegalArgumentException("The removerId is not the member"))
    else {
      if (!messages.filter(values).forall(_.senderId == removerId))
        Left(new IllegalArgumentException("The removerId is not the senderId"))
      else
        Right(copy(messages = messages.filterNot(values), updatedAt = at))
    }
  }

  def getMessages(senderId: AccountId): Either[Exception, Messages] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isMemberId(senderId))
      Left(new IllegalArgumentException("The senderId is not the member"))
    else
      Right(messages)
  }

  def destroy(senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new IllegalStateException("already removed the thread"))
    else if (!isAdministratorId(senderId))
      Left(new IllegalArgumentException("The senderId is not the administrator"))
    else
      Right(copy(removedAt = Some(at)))
  }

}
