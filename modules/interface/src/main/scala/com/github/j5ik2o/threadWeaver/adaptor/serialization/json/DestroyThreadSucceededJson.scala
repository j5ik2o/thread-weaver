package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.DestroyThreadSucceeded
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

case class DestroyThreadSucceededJson(id: String, requestId: String, threadId: String, createAt: Long)

object DestroyThreadSucceededJson {
  implicit object DestroyThreadSucceededIso
      extends DomainObjToJsonReprIso[DestroyThreadSucceeded, DestroyThreadSucceededJson] {
    override def convertTo(domainObj: DestroyThreadSucceeded): DestroyThreadSucceededJson =
      DestroyThreadSucceededJson(
        id = domainObj.id.asString,
        requestId = domainObj.requestId.asString,
        threadId = domainObj.threadId.asString,
        createAt = domainObj.createAt.toEpochMilli
      )

    override def convertFrom(json: DestroyThreadSucceededJson): DestroyThreadSucceeded =
      (
        for {
          id        <- ULID.parseFromString(json.id)
          requestId <- ULID.parseFromString(json.requestId)
          threadId  <- ULID.parseFromString(json.threadId).map(ThreadId)
        } yield (id, requestId, threadId)
      ).fold(throw _, {
        case (id, requestId, threadId) =>
          DestroyThreadSucceeded(id, requestId, threadId, Instant.ofEpochMilli(json.createAt))
      })
  }
}
