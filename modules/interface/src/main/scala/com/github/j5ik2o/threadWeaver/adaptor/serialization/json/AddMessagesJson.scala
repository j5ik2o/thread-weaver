package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.AddMessages
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{
  MessageId,
  Messages,
  Text,
  TextMessage,
  ThreadId,
  ToAccountIds
}
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

import scala.util.{ Success, Try }

case class AddMessagesJson(id: String, threadId: String, messages: Seq[TextMessageJson], createAt: Long, reply: Boolean)

object AddMessagesJson {
  implicit object AddMessagesIso extends DomainObjToJsonReprIso[AddMessages, AddMessagesJson] {
    override def convertTo(domainObj: AddMessages): AddMessagesJson = AddMessagesJson(
      id = domainObj.id.asString,
      threadId = domainObj.threadId.value.asString,
      messages = domainObj.messages.breachEncapsulationOfValues.map {
        case v: TextMessage =>
          TextMessageJson(
            v.id.value.asString,
            v.replyMessageId.map(_.value.asString),
            v.toAccountIds.breachEncapsulationOfValues.map(_.value.asString),
            v.`type`,
            v.body.toString,
            v.senderId.value.asString,
            v.createdAt.toEpochMilli,
            v.updatedAt.toEpochMilli
          )
      },
      createAt = domainObj.createAt.toEpochMilli,
      reply = domainObj.reply
    )
    override def convertFrom(json: AddMessagesJson): AddMessages =
      (for {
        id       <- ULID.parseFromString(json.id)
        threadId <- ULID.parseFromString(json.threadId)
        messages <- json.messages.foldLeft(Try(Seq.empty[TextMessage])) {
          case (result, element) =>
            for {
              r  <- result
              id <- ULID.parseFromString(element.id).map(MessageId)
              replyMessageId <- element.replyMessageId
                .map { v =>
                  ULID.parseFromString(v).map(MessageId).map(Some(_))
                }.getOrElse(Success(None))
              toAccountIds <- element.toAccountIds.foldLeft(Try(Seq.empty[AccountId])) {
                case (cr, ce) =>
                  for {
                    r <- cr
                    e <- ULID.parseFromString(ce)
                  } yield r :+ AccountId(e)
              }
              senderId <- ULID.parseFromString(element.senderId).map(AccountId)
            } yield
              r :+ TextMessage(
                id,
                replyMessageId,
                ToAccountIds(toAccountIds: _*),
                Text(element.body),
                senderId,
                Instant.ofEpochMilli(element.createdAt),
                Instant.ofEpochMilli(element.updatedAt)
              )
        }
      } yield (id, threadId, messages)).fold(throw _, {
        case (id, threadId, messages) =>
          AddMessages(id, ThreadId(threadId), Messages(messages: _*), Instant.ofEpochMilli(json.createAt), json.reply)
      })
  }
}
