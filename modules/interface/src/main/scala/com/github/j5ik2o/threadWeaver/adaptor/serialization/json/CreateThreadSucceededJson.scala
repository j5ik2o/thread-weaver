package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.CreateThreadSucceeded
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

case class CreateThreadSucceededJson(id: String, requestId: String, threadId: String, createAt: Long)

object CreateThreadSucceededJson {

  implicit object CreateThreadSucceededIso
      extends DomainObjToJsonReprIso[CreateThreadSucceeded, CreateThreadSucceededJson] {
    override def convertTo(domainObj: CreateThreadSucceeded): CreateThreadSucceededJson =
      CreateThreadSucceededJson(
        id = domainObj.id.asString,
        requestId = domainObj.requestId.asString,
        threadId = domainObj.threadId.value.asString,
        createAt = domainObj.createAt.toEpochMilli
      )

    override def convertFrom(json: CreateThreadSucceededJson): CreateThreadSucceeded = {
      (for {
        id        <- ULID.parseFromString(json.id)
        requestId <- ULID.parseFromString(json.requestId)
        threadId  <- ULID.parseFromString(json.threadId)
      } yield (id, requestId, threadId)).fold(throw _, {
        case (id, requestId, threadId) =>
          CreateThreadSucceeded(
            id,
            requestId,
            ThreadId(threadId),
            Instant.ofEpochMilli(json.createAt)
          )
      })
    }
  }

}
