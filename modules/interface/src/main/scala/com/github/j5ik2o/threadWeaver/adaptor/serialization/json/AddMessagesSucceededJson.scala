package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.{ AddMessages, AddMessagesSucceeded }
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ MessageId, MessageIds, ThreadId }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

import scala.util.Try

case class AddMessagesSucceededJson(
    id: String,
    requestId: String,
    threadId: String,
    messageIds: Seq[String],
    createAt: Long
)

object AddMessagesSucceededJson {
  implicit object AddMessagesSucceededIso
      extends DomainObjToJsonReprIso[AddMessagesSucceeded, AddMessagesSucceededJson] {
    override def convertTo(domainObj: AddMessagesSucceeded): AddMessagesSucceededJson =
      AddMessagesSucceededJson(
        id = domainObj.id.asString,
        requestId = domainObj.requestId.asString,
        threadId = domainObj.threadId.asString,
        messageIds = domainObj.messageIds.breachEncapsulationOfValues.map(_.value.asString),
        createAt = domainObj.createAt.toEpochMilli
      )

    override def convertFrom(json: AddMessagesSucceededJson): AddMessagesSucceeded = {
      (for {
        id        <- ULID.parseFromString(json.id)
        requestId <- ULID.parseFromString(json.requestId)
        threadId  <- ULID.parseFromString(json.threadId).map(ThreadId)
        messageIds <- json.messageIds.foldLeft(Try(Seq.empty[MessageId])) {
          case (result, element) =>
            for {
              r         <- result
              messageId <- ULID.parseFromString(element).map(MessageId)
            } yield r :+ messageId
        }
      } yield (id, requestId, threadId, messageIds)).fold(
        throw _, {
          case (id, requestId, threadId, messageIds) =>
            AddMessagesSucceeded(
              id,
              requestId,
              threadId,
              MessageIds(messageIds: _*),
              Instant.ofEpochMilli(json.createAt)
            )
        }
      )
    }
  }
}
