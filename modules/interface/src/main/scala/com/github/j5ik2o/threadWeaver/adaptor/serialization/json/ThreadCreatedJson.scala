package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.ThreadCreated
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

case class ThreadCreatedJson(
    id: String,
    threadId: String,
    creatorId: String,
    parentThreadId: Option[String],
    title: String,
    remarks: Option[String],
    administratorIds: Seq[String],
    memberIds: Seq[String],
    createdAt: Long
)

object ThreadCreatedJson {

  implicit object ThreadCreatedIso extends DomainObjToJsonReprIso[ThreadCreated, ThreadCreatedJson] {
    override def convertTo(domainObj: ThreadCreated): ThreadCreatedJson = {
      ThreadCreatedJson(
        id = domainObj.id.asString,
        threadId = domainObj.threadId.value.asString,
        creatorId = domainObj.creatorId.value.asString,
        parentThreadId = domainObj.parentThreadId.map(_.value.asString),
        title = domainObj.title.value,
        remarks = domainObj.remarks.map(_.value),
        administratorIds = domainObj.administratorIds.breachEncapsulationOfValues.toList.map(_.value.asString),
        memberIds = domainObj.memberIds.breachEncapsulationOfValues.map(_.value.asString),
        createdAt = domainObj.createdAt.toEpochMilli
      )
    }
    override def convertFrom(json: ThreadCreatedJson): ThreadCreated = {
      (for {
        eventId   <- ULID.parseFromString(json.id)
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
      } yield (eventId, threadId, creatorId, parentThreadId, administratorIds, memberIds)).fold(
        throw _, {
          case (eventId, threadId, creatorId, parentThreadId, administratorIds, memberIds) =>
            ThreadCreated(
              eventId,
              ThreadId(threadId),
              AccountId(creatorId),
              parentThreadId = parentThreadId.map(ThreadId),
              title = ThreadTitle(json.title),
              remarks = json.remarks.map(ThreadRemarks),
              administratorIds = AdministratorIds(administratorIds.head, administratorIds.tail.toList),
              memberIds = MemberIds(memberIds: _*),
              createdAt = Instant.ofEpochMilli(json.createdAt)
            )
        }
      )
    }
  }

}
