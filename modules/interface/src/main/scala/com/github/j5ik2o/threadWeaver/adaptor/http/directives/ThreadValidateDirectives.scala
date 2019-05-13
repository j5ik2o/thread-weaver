package com.github.j5ik2o.threadWeaver.adaptor.http.directives

import cats.implicits._
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import com.github.j5ik2o.threadWeaver.adaptor.http.json._
import com.github.j5ik2o.threadWeaver.adaptor.validator.{ ValidateUtils, ValidationResult, Validator }
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

trait ThreadValidateDirectives {

  protected def validateThreadId(value: String): Directive1[ThreadId] = {
    ValidateUtils
      .validateThreadId(value)
      .fold({ errors =>
        reject(ValidationsRejection(errors))
      }, provide)
  }

  protected def validateAccountId(value: String): Directive1[AccountId] = {
    ValidateUtils
      .validateAccountId(value)
      .fold({ errors =>
        reject(ValidationsRejection(errors))
      }, provide)
  }

  protected def validateRequestJson[A, B](value: A)(implicit V: Validator[A, B]): Directive1[B] =
    V.validate(value)
      .fold({ errors =>
        reject(ValidationsRejection(errors))
      }, provide)

}

object ThreadValidateDirectives {

  import ValidateUtils._

  implicit object CreateThreadRequestJsonValidator extends Validator[CreateThreadRequestJson, CreateThread] {
    override def validate(
        value: CreateThreadRequestJson
    ): ValidationResult[CreateThread] = {
      (
        validateAccountId(value.creatorId),
        validateThreadIdOpt(value.parentThreadId),
        validateThreadTitle(value.title),
        validateThreadRemarks(value.remarks),
        validateAdministratorIds(value.administratorIds),
        validateMemberIds(value.memberIds),
        validateInstant(value.createAt)
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

  implicit object JoinAdministratorIdsRequestJsonValidator
      extends Validator[(ThreadId, JoinAdministratorIdsRequestJson), JoinAdministratorIds] {
    override def validate(
        value: (ThreadId, JoinAdministratorIdsRequestJson)
    ): ValidationResult[JoinAdministratorIds] = {
      (
        validateAccountId(value._2.adderId),
        validateAdministratorIds(value._2.accountIds),
        validateInstant(value._2.createAt)
      ).mapN {
        case (adderId, administratorIds, createdAt) =>
          JoinAdministratorIds(ULID(), value._1, adderId, administratorIds, createdAt)
      }
    }
  }

  implicit object LeaveAdministratorIdsRequestJsonValidator
      extends Validator[(ThreadId, LeaveAdministratorIdsRequestJson), LeaveAdministratorIds] {
    override def validate(
        value: (ThreadId, LeaveAdministratorIdsRequestJson)
    ): ValidationResult[LeaveAdministratorIds] = {
      (
        validateAccountId(value._2.removerId),
        validateAdministratorIds(value._2.administratorIds),
        validateInstant(value._2.createAt)
      ).mapN {
        case (adderId, administratorIds, createdAt) =>
          LeaveAdministratorIds(ULID(), value._1, adderId, administratorIds, createdAt)
      }
    }
  }

  implicit object JoinMemberIdsRequestJsonValidator
      extends Validator[(ThreadId, JoinMemberIdsRequestJson), JoinMemberIds] {
    override def validate(value: (ThreadId, JoinMemberIdsRequestJson)): ValidationResult[JoinMemberIds] = {
      (
        validateAccountId(value._2.adderId),
        validateMemberIds(value._2.accountIds),
        validateInstant(value._2.createAt)
      ).mapN {
        case (adderId, memberIds, createdAt) =>
          JoinMemberIds(ULID(), value._1, adderId, memberIds, createdAt)
      }
    }
  }

  implicit object LeaveMemberIdsRequestJsonValidator
      extends Validator[(ThreadId, LeaveMemberIdsRequestJson), LeaveMemberIds] {
    override def validate(value: (ThreadId, LeaveMemberIdsRequestJson)): ValidationResult[LeaveMemberIds] = {
      (
        validateAccountId(value._2.removerId),
        validateMemberIds(value._2.accountIds),
        validateInstant(value._2.createAt)
      ).mapN {
        case (adderId, memberIds, createdAt) =>
          LeaveMemberIds(ULID(), value._1, adderId, memberIds, createdAt)
      }
    }
  }

  implicit object AddMessagesRequestJsonValidator extends Validator[(ThreadId, AddMessagesRequestJson), AddMessages] {
    override def validate(value: (ThreadId, AddMessagesRequestJson)): ValidationResult[AddMessages] = {
      (
        value._2.messages
          .map { v =>
            validateTextMessage(v.replyMessageId, v.toAccountIds, v.text, value._2.senderId)
          }.toList.sequence,
        validateInstant(value._2.createAt)
      ).mapN {
        case (textMessages, createdAt) =>
          AddMessages(ULID(), value._1, textMessages, createdAt)
      }
    }
  }

  implicit object RemoveMessagesRequestJsonValidator
      extends Validator[(ThreadId, RemoveMessagesRequestJson), RemoveMessages] {
    override def validate(value: (ThreadId, RemoveMessagesRequestJson)): ValidationResult[RemoveMessages] = {
      (
        validateAccountId(value._2.senderId),
        validateMessageIds(value._2.messageIds),
        validateInstant(value._2.createAt)
      ).mapN {
        case (adderId, messageIds, createdAt) =>
          RemoveMessages(ULID(), value._1, adderId, messageIds, createdAt)
      }
    }
  }

}
