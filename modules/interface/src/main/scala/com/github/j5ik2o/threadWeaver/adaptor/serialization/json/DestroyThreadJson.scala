package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.DestroyThread
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

case class DestroyThreadJson(id: String, threadId: String, destroyerId: String, createAt: Long, reply: Boolean)

object DestroyThreadJson {

  implicit object DestroyThreadIso extends DomainObjToJsonReprIso[DestroyThread, DestroyThreadJson] {
    override def convertTo(domainObj: DestroyThread): DestroyThreadJson =
      DestroyThreadJson(
        id = domainObj.id.asString,
        threadId = domainObj.threadId.asString,
        destroyerId = domainObj.destroyerId.value.asString,
        createAt = domainObj.createAt.toEpochMilli,
        reply = domainObj.reply
      )
    override def convertFrom(json: DestroyThreadJson): DestroyThread =
      (
        for {
          id          <- ULID.parseFromString(json.id)
          threadId    <- ULID.parseFromString(json.threadId).map(ThreadId)
          destroyerId <- ULID.parseFromString(json.destroyerId).map(AccountId)
        } yield (id, threadId, destroyerId)
      ).fold(throw _, {
        case (id, threadId, destroyerId) =>
          DestroyThread(id, threadId, destroyerId, Instant.ofEpochMilli(json.createAt), json.reply)
      })
  }

}
