package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.CreateThreadFailed
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

case class CreateThreadFailedJson(id: String, requestId: String, threadId: String, message: String, createAt: Long)

object CreateThreadFailedJson {
  implicit object CreateThreadFailedIso extends DomainObjToJsonReprIso[CreateThreadFailed, CreateThreadFailedJson] {
    override def convertTo(domainObj: CreateThreadFailed): CreateThreadFailedJson =
      CreateThreadFailedJson(
        id = domainObj.id.asString,
        requestId = domainObj.requestId.asString,
        threadId = domainObj.threadId.value.asString,
        message = domainObj.message,
        createAt = domainObj.createAt.toEpochMilli
      )

    override def convertFrom(json: CreateThreadFailedJson): CreateThreadFailed =
      (for {
        id        <- ULID.parseFromString(json.id)
        requestId <- ULID.parseFromString(json.requestId)
        threadId  <- ULID.parseFromString(json.threadId)
      } yield (id, requestId, threadId)).fold(throw _, {
        case (id, requestId, threadId) =>
          CreateThreadFailed(
            id,
            requestId,
            ThreadId(threadId),
            json.message,
            Instant.ofEpochMilli(json.createAt)
          )
      })
  }
}
