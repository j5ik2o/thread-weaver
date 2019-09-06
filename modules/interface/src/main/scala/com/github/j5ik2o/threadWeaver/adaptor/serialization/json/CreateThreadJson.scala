package com.github.j5ik2o.threadWeaver.adaptor.serialization.json
import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.CreateThread
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{
  AdministratorIds,
  MemberIds,
  ThreadId,
  ThreadRemarks,
  ThreadTitle
}
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

import scala.util.{ Success, Try }

case class CreateThreadJson(
    id: String,
    threadId: String,
    creatorId: String,
    parentThreadId: Option[String],
    title: String,
    remarks: Option[String],
    administratorIds: Seq[String],
    memberIds: Seq[String],
    createAt: Long,
    reply: Boolean
)

object CreateThreadJson {

  implicit object CreateThreadIso extends DomainObjToJsonReprIso[CreateThread, CreateThreadJson] {
    override def convertTo(domainObj: CreateThread): CreateThreadJson =
      CreateThreadJson(
        id = domainObj.id.asString,
        threadId = domainObj.threadId.value.asString,
        creatorId = domainObj.creatorId.value.asString,
        parentThreadId = domainObj.parentThreadId.map(_.value.asString),
        title = domainObj.title.value,
        remarks = domainObj.remarks.map(_.value),
        administratorIds = domainObj.administratorIds.valuesAsString,
        memberIds = domainObj.memberIds.valuesAsString,
        createAt = domainObj.createAt.toEpochMilli,
        reply = domainObj.reply
      )

    override def convertFrom(json: CreateThreadJson): CreateThread = {
      (for {
        id        <- ULID.parseFromString(json.id)
        threadId  <- ULID.parseFromString(json.threadId)
        creatorId <- ULID.parseFromString(json.creatorId)
        parentThreadId <- Try(json.parentThreadId).flatMap {
          case None    => Success(None)
          case Some(v) => ULID.parseFromString(v).map(Option(_))
        }
        administratorIds <- json.administratorIds.foldLeft(Try(Seq.empty[AccountId])) {
          case (result, element) =>
            for {
              r <- result
              e <- ULID.parseFromString(element)
            } yield r :+ AccountId(e)
        }
        memberIds <- json.memberIds.foldLeft(Try(Seq.empty[AccountId])) {
          case (result, element) =>
            for {
              r <- result
              e <- ULID.parseFromString(element)
            } yield r :+ AccountId(e)
        }
      } yield (id, threadId, creatorId, parentThreadId, administratorIds, memberIds)).fold(
        throw _, {
          case (id, threadId, creatorId, parentThreadId, administratorIds, memberIds) =>
            CreateThread(
              id,
              ThreadId(threadId),
              AccountId(creatorId),
              parentThreadId.map(ThreadId),
              ThreadTitle(json.title),
              json.remarks.map(ThreadRemarks),
              AdministratorIds(administratorIds.head, administratorIds.tail.toList),
              MemberIds(memberIds: _*),
              Instant.ofEpochMilli(json.createAt),
              json.reply
            )
        }
      )
    }
  }

}
