package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.ExistsThreadFailed
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

case class ExistsThreadFailedJson(id: String, requestId: String, threadId: String, message: String, createAt: Long)

object ExistsThreadFailedJson {

  implicit object ExistsThreadFailedIso extends DomainObjToJsonReprIso[ExistsThreadFailed, ExistsThreadFailedJson] {
    override def convertTo(domainObj: ExistsThreadFailed): ExistsThreadFailedJson =
      ExistsThreadFailedJson(
        id = domainObj.id.asString,
        requestId = domainObj.requestId.asString,
        threadId = domainObj.threadId.asString,
        message = domainObj.message,
        createAt = domainObj.createAt.toEpochMilli
      )

    override def convertFrom(json: ExistsThreadFailedJson): ExistsThreadFailed =
      (for {
        id        <- ULID.parseFromString(json.id)
        requestId <- ULID.parseFromString(json.requestId)
        threadId  <- ULID.parseFromString(json.threadId).map(ThreadId)
      } yield (id, requestId, threadId)).fold(throw _, {
        case (id, requestId, threadId) =>
          ExistsThreadFailed(id, requestId, threadId, json.message, Instant.ofEpochMilli(json.createAt))
      })
  }

}
