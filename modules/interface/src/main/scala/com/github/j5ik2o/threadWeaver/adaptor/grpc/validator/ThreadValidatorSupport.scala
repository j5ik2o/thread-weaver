package com.github.j5ik2o.threadWeaver.adaptor.grpc.validator

import cats.implicits._
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.{ CreateThreadRequest, JoinAdministratorIdsRequest }
import com.github.j5ik2o.threadWeaver.adaptor.validator.ValidateUtils._
import com.github.j5ik2o.threadWeaver.adaptor.validator.{ ValidationResult, Validator }
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ CreateThread, JoinAdministratorIds }

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
        validateAccountId(in.creatorId),
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

  implicit object JoinAdministratorIdsRequestJsonValidator
      extends Validator[(ThreadId, JoinAdministratorIdsRequest), JoinAdministratorIds] {
    override def validate(
        value: (ThreadId, JoinAdministratorIdsRequest)
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
}
