package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.AddMessagesFailed
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

case class AddMessagesFailedJson(
    id: String,
    requestId: String,
    threadId: String,
    message: String,
    createAt: Long
)

object AddMessagesFailedJson {
  implicit object AddMessagesSucceededIso extends DomainObjToJsonReprIso[AddMessagesFailed, AddMessagesFailedJson] {
    override def convertTo(domainObj: AddMessagesFailed): AddMessagesFailedJson =
      AddMessagesFailedJson(
        id = domainObj.id.asString,
        requestId = domainObj.requestId.asString,
        threadId = domainObj.threadId.asString,
        message = domainObj.message,
        createAt = domainObj.createAt.toEpochMilli
      )

    override def convertFrom(json: AddMessagesFailedJson): AddMessagesFailed =
      (for {
        id        <- ULID.parseFromString(json.id)
        requestId <- ULID.parseFromString(json.requestId)
        threadId  <- ULID.parseFromString(json.threadId).map(ThreadId)
      } yield (id, requestId, threadId)).fold(throw _, {
        case (id, requestId, threadId) =>
          AddMessagesFailed(id, requestId, threadId, json.message, Instant.ofEpochMilli(json.createAt))
      })
  }
}
