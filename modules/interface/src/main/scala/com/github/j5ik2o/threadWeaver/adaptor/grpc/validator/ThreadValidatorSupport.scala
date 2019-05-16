package com.github.j5ik2o.threadWeaver.adaptor.grpc.validator

import cats.implicits._
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model._
import com.github.j5ik2o.threadWeaver.adaptor.validator.ValidateUtils._
import com.github.j5ik2o.threadWeaver.adaptor.validator.{ ValidationResult, Validator }
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

trait ThreadValidatorSupport {

  protected def validateGrpcRequest[A, B](value: A)(implicit V: Validator[A, B]): ValidationResult[B] =
    V.validate(value)

}

object ThreadValidatorSupport {

  implicit object CreateThreadRequestJsonValidator extends Validator[CreateThreadRequest, CreateThread] {
    override def validate(
        in: CreateThreadRequest
    ): ValidationResult[CreateThread] = {
      (
        validateAccountId(in.accountId),
        validateThreadIdOpt(if (in.hasParentId) Some(in.parentId) else None),
        validateThreadTitle(in.title),
        validateThreadRemarks(if (in.hasRemarks) Some(in.remarks) else None),
        validateAdministratorIds(in.administratorIds),
        validateMemberIds(in.memberIds),
        validateInstant(in.createAt)
      ).mapN {
        case (creatorId, parentThreadId, title, remarks, administratorIds, memberIds, createdAt) =>
          CreateThread(
            ULID(),
            ThreadId(),
            creatorId,
            parentThreadId,
            title,
            remarks,
            administratorIds,
            memberIds,
            createdAt
          )
      }
    }
  }

  implicit object DestroyThreadRequestJsonValidator extends Validator[DestroyThreadRequest, DestroyThread] {
    override def validate(
        in: DestroyThreadRequest
    ): ValidationResult[DestroyThread] = {
      (
        validateThreadId(in.threadId),
        validateAccountId(in.accountId),
        validateInstant(in.createAt)
      ).mapN {
        case (threadId, accountId, createdAt) =>
          DestroyThread(
            ULID(),
            threadId,
            accountId,
            createdAt
          )
      }
    }
  }

  implicit object JoinAdministratorIdsRequestJsonValidator
      extends Validator[JoinAdministratorIdsRequest, JoinAdministratorIds] {
    override def validate(
        value: JoinAdministratorIdsRequest
    ): ValidationResult[JoinAdministratorIds] = {
      (
        validateThreadId(value.threadId),
        validateAccountId(value.accountId),
        validateAdministratorIds(value.accountIds),
        validateInstant(value.createAt)
      ).mapN {
        case (threadId, accountId, administratorIds, createdAt) =>
          JoinAdministratorIds(ULID(), threadId, accountId, administratorIds, createdAt)
      }
    }
  }

  implicit object LeaveAdministratorIdsRequestJsonValidator
      extends Validator[LeaveAdministratorIdsRequest, LeaveAdministratorIds] {
    override def validate(
        value: LeaveAdministratorIdsRequest
    ): ValidationResult[LeaveAdministratorIds] = {
      (
        validateThreadId(value.threadId),
        validateAccountId(value.accountId),
        validateAdministratorIds(value.accountIds),
        validateInstant(value.createAt)
      ).mapN {
        case (threadId, accountId, administratorIds, createdAt) =>
          LeaveAdministratorIds(ULID(), threadId, accountId, administratorIds, createdAt)
      }
    }
  }

  implicit object JoinMemberIdsRequestJsonValidator extends Validator[JoinMemberIdsRequest, JoinMemberIds] {
    override def validate(
        value: JoinMemberIdsRequest
    ): ValidationResult[JoinMemberIds] = {
      (
        validateThreadId(value.threadId),
        validateAccountId(value.accountId),
        validateMemberIds(value.accountIds),
        validateInstant(value.createAt)
      ).mapN {
        case (threadId, accountId, memberIds, createdAt) =>
          JoinMemberIds(ULID(), threadId, accountId, memberIds, createdAt)
      }
    }
  }

  implicit object LeaveMemberIdsRequestJsonValidator extends Validator[LeaveMemberIdsRequest, LeaveMemberIds] {
    override def validate(
        value: LeaveMemberIdsRequest
    ): ValidationResult[LeaveMemberIds] = {
      (
        validateThreadId(value.threadId),
        validateAccountId(value.accountId),
        validateMemberIds(value.accountIds),
        validateInstant(value.createAt)
      ).mapN {
        case (threadId, accountId, memberIds, createdAt) =>
          LeaveMemberIds(ULID(), threadId, accountId, memberIds, createdAt)
      }
    }
  }

  implicit object AddMessagesRequestJsonValidator extends Validator[AddMessagesRequest, AddMessages] {
    override def validate(
        in: AddMessagesRequest
    ): ValidationResult[AddMessages] = {
      (
        validateThreadId(in.threadId),
        in.messages
          .map { v =>
            validateTextMessage(
              if (v.hasReplyMessageId) Some(v.replyMessageId) else None,
              v.toAccountIds,
              v.text,
              in.accountId
            )
          }.toList.sequence,
        validateInstant(in.createAt)
      ).mapN {
        case (threadId, textMessages, createdAt) =>
          AddMessages(ULID(), threadId, textMessages, createdAt)
      }
    }
  }

  implicit object RemoveMessagesRequestJsonValidator extends Validator[RemoveMessagesRequest, RemoveMessages] {
    override def validate(
        in: RemoveMessagesRequest
    ): ValidationResult[RemoveMessages] = {
      (
        validateThreadId(in.threadId),
        validateAccountId(in.accountId),
        validateMessageIds(in.messageIds),
        validateInstant(in.createAt)
      ).mapN {
        case (threadId, accountId, messageIds, createdAt) =>
          RemoveMessages(ULID(), threadId, accountId, messageIds, createdAt)
      }
    }
  }

}
