package com.github.j5ik2o.threadWeaver.domain.model.threads

import java.time.Instant

import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import cats.implicits._

final case class Thread(
    id: ThreadId,
    creatorId: AccountId,
    parentThreadId: Option[ThreadId],
    administratorIds: AdministratorIds,
    memberIds: MemberIds,
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

  def addAdministratorIds(value: AdministratorIds, senderId: AccountId): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new Exception("already removed thread"))
    else if (!isAdministratorId(senderId))
      Left(new Exception("senderId is not administrator"))
    else
      Right(copy(administratorIds = administratorIds combine value))
  }

  def addMemberIds(value: MemberIds, senderId: AccountId): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new Exception("already removed thread"))
    else if (!isAdministratorId(senderId))
      Left(new Exception("senderId is not administrator."))
    else
      Right(copy(memberIds = memberIds combine value))
  }

  def addMessages(values: Messages, senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new Exception("already removed thread"))
    else if (!isMemberId(senderId))
      Left(new Exception("senderId is not member"))
    else
      Right(copy(messages = messages combine values, updatedAt = at))
  }

  def getMessages(senderId: AccountId): Either[Exception, Messages] = {
    if (isRemoved)
      Left(new Exception("already removed thread"))
    else if (!isMemberId(senderId))
      Left(new Exception("senderId is not member"))
    else
      Right(messages)
  }

  def filterNotMessages(values: MessageIds, senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new Exception("already removed thread"))
    else if (!isAdministratorId(senderId))
      Left(new Exception("senderId is not administrator."))
    else
      Right(copy(messages = messages.filterNot(values, senderId)))
  }

  def destroy(senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isRemoved)
      Left(new Exception("already removed thread"))
    else if (!isAdministratorId(senderId))
      Left(new Exception("senderId is not administrator."))
    else
      Right(copy(removedAt = Some(at)))
  }

}
