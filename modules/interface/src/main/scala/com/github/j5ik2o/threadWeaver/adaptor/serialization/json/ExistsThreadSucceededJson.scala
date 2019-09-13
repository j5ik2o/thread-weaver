package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.{
  AddMessagesSucceeded,
  ExistsThreadSucceeded
}
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

case class ExistsThreadSucceededJson(
    id: String,
    requestId: String,
    threadId: String,
    exists: Boolean,
    createAt: Long
)

object ExistsThreadSucceededJson {
  implicit object ExistsThreadSucceededIso
      extends DomainObjToJsonReprIso[ExistsThreadSucceeded, ExistsThreadSucceededJson] {
    override def convertTo(domainObj: ExistsThreadSucceeded): ExistsThreadSucceededJson =
      ExistsThreadSucceededJson(
        domainObj.id.asString,
        domainObj.requestId.asString,
        domainObj.threadId.asString,
        domainObj.exists,
        domainObj.createAt.toEpochMilli
      )

    override def convertFrom(json: ExistsThreadSucceededJson): ExistsThreadSucceeded =
      (
        for {
          id        <- ULID.parseFromString(json.id)
          requestId <- ULID.parseFromString(json.requestId)
          threadId  <- ULID.parseFromString(json.threadId).map(ThreadId)
        } yield (id, requestId, threadId)
      ).fold(throw _, {
        case (id, requestId, threadId) =>
          ExistsThreadSucceeded(id, requestId, threadId, json.exists, Instant.ofEpochMilli(json.createAt))
      })
  }
}
