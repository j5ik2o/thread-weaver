package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.{
  DestroyThreadSucceeded,
  JoinAdministratorIds
}
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ AdministratorIds, ThreadId }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

import scala.util.Try

case class JoinAdministratorIdsJson(
    id: String,
    threadId: String,
    adderId: String,
    administratorIds: Seq[String],
    createAt: Long,
    reply: Boolean
)

object JoinAdministratorIdsJson {

  implicit object JoinAdministratorIdsIso
      extends DomainObjToJsonReprIso[JoinAdministratorIds, JoinAdministratorIdsJson] {

    override def convertTo(domainObj: JoinAdministratorIds): JoinAdministratorIdsJson =
      JoinAdministratorIdsJson(
        id = domainObj.id.asString,
        threadId = domainObj.threadId.asString,
        adderId = domainObj.adderId.asString,
        administratorIds = domainObj.administratorIds.valuesAsString,
        createAt = domainObj.createAt.toEpochMilli,
        reply = domainObj.reply
      )

    override def convertFrom(json: JoinAdministratorIdsJson): JoinAdministratorIds =
      (
        for {
          id       <- ULID.parseFromString(json.id)
          threadId <- ULID.parseFromString(json.threadId).map(ThreadId)
          adderId  <- ULID.parseFromString(json.adderId).map(AccountId)
          administratorIds <- json.administratorIds.foldLeft(Try(Seq.empty[AccountId])) {
            case (result, element) =>
              for {
                r  <- result
                id <- ULID.parseFromString(element).map(AccountId)
              } yield r :+ id
          }
        } yield (id, threadId, adderId, administratorIds)
      ).fold(
        throw _, {
          case (id, threadId, adderId, administratorIds) =>
            JoinAdministratorIds(
              id,
              threadId,
              adderId,
              AdministratorIds(administratorIds.head, administratorIds.tail: _*),
              Instant.ofEpochMilli(json.createAt),
              json.reply
            )
        }
      )

  }
}
