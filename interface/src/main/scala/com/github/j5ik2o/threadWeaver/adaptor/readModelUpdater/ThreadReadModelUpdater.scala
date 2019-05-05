package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ Behavior, PostStop }
import akka.persistence.query.scaladsl._
import akka.stream.scaladsl.{ Keep, Source }
import akka.stream.typed.scaladsl.{ ActorMaterializer, ActorSink }
import akka.stream.{ Attributes, KillSwitch, KillSwitches }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.{
  CommandRequest => _,
  Message => _,
  Stop => _,
  _
}
import com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc.{
  ThreadAdministratorIdsComponent,
  ThreadComponent,
  ThreadMemberIdsComponent,
  ThreadMessageComponent
}
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ AdministratorIds, MemberIds, Messages, ThreadId }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import slick.jdbc.JdbcProfile
import slick.sql.FixedSqlAction

object ThreadReadModelUpdater {

  type ReadJournalType = ReadJournal
    with PersistenceIdsQuery
    with CurrentPersistenceIdsQuery
    with EventsByPersistenceIdQuery
    with CurrentEventsByPersistenceIdQuery
    with EventsByTagQuery
    with CurrentEventsByTagQuery
}

class ThreadReadModelUpdater(
    val readJournal: ReadJournalType,
    val profile: JdbcProfile,
    val db: JdbcProfile#Backend#Database
) extends ThreadComponent
    with ThreadMessageComponent
    with ThreadAdministratorIdsComponent
    with ThreadMemberIdsComponent {
  import profile.api._

  def behavior: Behavior[CommandRequest] = Behaviors.setup[CommandRequest] { ctx =>
    Behaviors.receiveMessagePartial[CommandRequest] {
      case s: Start =>
        ctx.child(s.threadId.value.asString) match {
          case None =>
            ctx.spawn(projection(s.threadId), name = s"RMU-${s.threadId.value.asString}") ! s
          case _ =>
//          case Some(childRef) =>
//            childRef.asInstanceOf[ActorRef[ThreadId]]
        }
        Behaviors.same
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private def projection(threadId: ThreadId): Behavior[Message] =
    Behaviors.setup[Message] { ctx =>
      implicit val system                = ctx.system
      implicit val ec                    = ctx.executionContext
      implicit val mat                   = ActorMaterializer()
      var killSwitch: Option[KillSwitch] = None

      Behaviors
        .receiveMessagePartial[Message] {
          case EventMessage(_, sequenceNr, event) =>
            event match {
              case ThreadCreated(_, _, senderId, parentThreadId, administratorIds, memberIds, createdAt) =>
                val insertThread = insertThreadQuery(threadId, sequenceNr, senderId, parentThreadId, createdAt)
                val insertAdministratorIds =
                  insertAdministratorIdsQuery(threadId, senderId, administratorIds, createdAt)
                val insertMemberIds = insertMemberIdsQuery(threadId, senderId, memberIds, createdAt)
                db.run(
                  DBIO
                    .seq(insertThread :: insertAdministratorIds ::: insertMemberIds: _*).transactionally
                )
              case ThreadDestroyed(_, _, _, createdAt) =>
                db.run(ThreadDao.filter(_.id === threadId.value.asString).map(_.removedAt).update(Some(createdAt)))
              case AdministratorIdsAdded(_, _, senderId, administratorIds, createdAt) =>
                db.run(DBIO.seq(insertAdministratorIdsQuery(threadId, senderId, administratorIds, createdAt): _*))
              case MemberIdsAdded(_, _, adderId, memberIds, createdAt) =>
                db.run(DBIO.seq(insertMemberIdsQuery(threadId, adderId, memberIds, createdAt): _*))
              case MessagesAdded(_, _, senderId, messages, createdAt) =>
                db.run(DBIO.seq(insertMessages(threadId, senderId, messages, createdAt): _*))
              case MessagesRemoved(_, _, messageIds, _, createdAt) =>
                db.run(
                  ThreadMessageDao
                    .filter(_.id.inSet(messageIds.breachEncapsulationOfValues.map(_.value.asString))).map { v =>
                      (v.deleted, v.updatedAt)
                    }.update(
                      (true, createdAt)
                    )
                )
            }
            Behaviors.same
          case _: Start =>
            killSwitch = Some(
              Source
                .fromFuture(
                  db.run(ThreadDao.filter(_.id === threadId.value.asString).map(_.sequenceNr).max.result).map(
                      _.getOrElse(0L)
                    )
                ).flatMapConcat { lastSequenceNr =>
                  readJournal
                    .eventsByPersistenceId(threadId.value.asString, lastSequenceNr, Long.MaxValue)
                }.map { ee =>
                  EventMessage(ee.persistenceId, ee.sequenceNr, ee.event.asInstanceOf[Event])
                }
                .viaMat(
                  KillSwitches.single
                )(
                  Keep.right
                ).toMat(
                  ActorSink
                    .actorRef[Message](ref = ctx.self, onCompleteMessage = Complete, onFailureMessage = Fail.apply)
                )(Keep.left).withAttributes(
                  Attributes.logLevels(
                    onElement = Attributes.LogLevels.Info,
                    onFailure = Attributes.LogLevels.Error,
                    onFinish = Attributes.LogLevels.Info
                  )
                ).run()
            )
            Behaviors.same
          case Complete =>
            ctx.log.info("stream has completed")
            Behaviors.stopped
          case Fail(t) =>
            ctx.log.error("occurred error: {}", t)
            Behaviors.stopped
          case _: Stop =>
            Behaviors.stopped
        }.receiveSignal {
          case (_, PostStop) =>
            killSwitch.foreach(_.shutdown())
            Behaviors.same
        }
    }

  private def insertMessages(threadId: ThreadId, senderId: AccountId, messages: Messages, createdAt: Instant) = {
    messages.breachEncapsulationOfValues.map { message =>
      ThreadMessageDao += ThreadMessageRecord(
        id = message.id.value.asString,
        deleted = false,
        threadId = threadId.value.asString,
        senderId = senderId.value.asString,
        `type` = message.`type`,
        body = message.body.toString,
        createdAt = createdAt,
        updatedAt = createdAt
      )
    }
  }

  private def insertMemberIdsQuery(
      threadId: ThreadId,
      adderId: AccountId,
      memberIds: MemberIds,
      createdAt: Instant
  ): List[FixedSqlAction[Int, NoStream, slick.dbio.Effect.Write]] = {
    memberIds.breachEncapsulationOfValues.map { accountId =>
      ThreadMemberIdsDao += ThreadMemberIdsRecord(
        id = ULID().asString,
        threadId = threadId.value.asString,
        accountId = accountId.value.asString,
        adderId = adderId.value.asString,
        createdAt = createdAt,
        updatedAt = createdAt
      )
    }.toList
  }

  private def insertAdministratorIdsQuery(
      threadId: ThreadId,
      adderId: AccountId,
      administratorIds: AdministratorIds,
      createdAt: Instant
  ): List[FixedSqlAction[Int, NoStream, slick.dbio.Effect.Write]] = {
    administratorIds.breachEncapsulationOfValues.map { accountId =>
      ThreadAdministratorIdsDao += ThreadAdministratorIdsRecord(
        id = ULID().asString,
        threadId = threadId.value.asString,
        accountId = accountId.value.asString,
        adderId = adderId.value.asString,
        createdAt = createdAt,
        updatedAt = createdAt
      )
    }.toList
  }

  private def insertThreadQuery(
      threadId: ThreadId,
      sequenceNr: Long,
      senderId: AccountId,
      parentThreadId: Option[ThreadId],
      createdAt: Instant
  ): FixedSqlAction[Int, NoStream, slick.dbio.Effect.Write] = {
    ThreadDao += ThreadRecord(
      id = threadId.value.asString,
      deleted = false,
      sequenceNr = sequenceNr,
      creatorId = senderId.value.asString,
      parentId = parentThreadId.map(_.value.asString),
      createdAt = createdAt,
      updatedAt = createdAt,
      removedAt = None
    )
  }
}
