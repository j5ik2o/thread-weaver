package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.ExistsThread
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

case class ExistsThreadJson(
    id: String,
    threadId: String,
    senderId: String,
    createAt: Long
)

object ExistsThreadJson {

  implicit object ExistsThreadIso extends DomainObjToJsonReprIso[ExistsThread, ExistsThreadJson] {
    override def convertTo(domainObj: ExistsThread): ExistsThreadJson =
      ExistsThreadJson(
        id = domainObj.id.asString,
        threadId = domainObj.threadId.asString,
        senderId = domainObj.senderId.value.asString,
        domainObj.createAt.toEpochMilli
      )

    override def convertFrom(json: ExistsThreadJson): ExistsThread =
      (for {
        id       <- ULID.parseFromString(json.id)
        threadId <- ULID.parseFromString(json.threadId).map(ThreadId)
        senderId <- ULID.parseFromString(json.senderId).map(AccountId)
      } yield (id, threadId, senderId)).fold(throw _, {
        case (id, threadId, senderId) =>
          ExistsThread(id, threadId, senderId, Instant.ofEpochMilli(json.createAt))
      })
  }
}
