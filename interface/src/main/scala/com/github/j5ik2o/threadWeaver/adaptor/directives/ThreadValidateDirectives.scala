package com.github.j5ik2o.threadWeaver.adaptor.directives

import cats.implicits._
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import com.github.j5ik2o.threadWeaver.adaptor.json.{ AddAdministratorIdsRequestJson, CreateThreadRequestJson }
import com.github.j5ik2o.threadWeaver.adaptor.validator.{ ValidateUtils, ValidationResult, Validator }
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ AddAdministratorIds, CreateThread }

trait ThreadValidateDirectives {

  protected def validateThreadId(value: String): Directive1[ThreadId] = {
    ValidateUtils
      .validateThreadId(value)
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
        validateAdministratorIds(value.administratorIds),
        validateMemberIds(value.memberIds),
        validateInstant(value.createAt)
      ).mapN {
        case (creatorId, parentThreadId, administratorIds, memberIds, createdAt) =>
          CreateThread(ULID(), ThreadId(), creatorId, parentThreadId, administratorIds, memberIds, createdAt)
      }
    }
  }

  implicit object AddAdministratorIdsRequestJsonValidator
      extends Validator[(ThreadId, AddAdministratorIdsRequestJson), AddAdministratorIds] {
    override def validate(value: (ThreadId, AddAdministratorIdsRequestJson)): ValidationResult[AddAdministratorIds] = {
      (
        validateAccountId(value._2.adderId),
        validateAdministratorIds(value._2.administratorIds),
        validateInstant(value._2.createAt)
      ).mapN {
        case (adderId, administratorIds, createdAt) =>
          AddAdministratorIds(ULID(), value._1, adderId, administratorIds, createdAt)
      }
    }
  }

}
