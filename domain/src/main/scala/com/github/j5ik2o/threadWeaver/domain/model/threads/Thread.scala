package com.github.j5ik2o.threadWeaver.domain.model.threads

import java.time.Instant

import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import org.slf4j.LoggerFactory
import cats.implicits._

final case class Thread(
    id: ThreadId,
    parentThreadId: Option[ThreadId],
    administratorIds: AdministratorIds,
    memberIds: MemberIds,
    messages: Messages,
    createdAt: Instant,
    updatedAt: Instant
) {

  val logger = LoggerFactory.getLogger(getClass)

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

  def addMessages(values: Messages, senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isMemberId(senderId)) {
      logger.info(s"addMessages: messages = $messages, values = $values")
      Right(copy(messages = messages combine values, updatedAt = at))
    } else
      Left(new Exception("senderId is not member"))
  }

  def filterNotMessages(values: MessageIds, senderId: AccountId, at: Instant): Either[Exception, Thread] = {
    if (isAdministratorId(senderId))
      Right(copy(messages = messages.filterNot(values, senderId)))
    else
      Left(new Exception())
  }

}
