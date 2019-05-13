package com.github.j5ik2o.threadWeaver.adaptor.grpc.validator

import cats.implicits._
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.CreateThreadRequest
import com.github.j5ik2o.threadWeaver.adaptor.validator.ValidateUtils._
import com.github.j5ik2o.threadWeaver.adaptor.validator.ValidationResult
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.CreateThread

object ThreadValidateUtils {

  def validateCreateThreadRequest(in: CreateThreadRequest): ValidationResult[CreateThread] = {
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
