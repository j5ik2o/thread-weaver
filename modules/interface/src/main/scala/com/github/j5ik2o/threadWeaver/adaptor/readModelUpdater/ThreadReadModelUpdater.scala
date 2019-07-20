package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ Behavior, PostStop }
import akka.persistence.query.EventEnvelope
import akka.persistence.query.scaladsl._
import akka.stream.scaladsl.{ Flow, Keep, RestartSource, Sink, Source }
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.stream.{ Attributes, KillSwitch, KillSwitches }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadCommonProtocol
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadAggregate
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.{ CommandRequest => _, _ }
import com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc.{
  ThreadAdministratorIdsComponent,
  ThreadComponent,
  ThreadMemberIdsComponent,
  ThreadMessageComponent
}
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.{
  BackoffSettings,
  ReadJournalType
}
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ Message => _, _ }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import kamon.Kamon
import slick.jdbc.JdbcProfile
import slick.sql.FixedSqlAction

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

@SuppressWarnings(Array("org.wartremover.warts.Var"))
object ThreadReadModelUpdater {

  type ReadJournalType = ReadJournal
    with PersistenceIdsQuery
    with CurrentPersistenceIdsQuery
    with EventsByPersistenceIdQuery
    with CurrentEventsByPersistenceIdQuery
    with EventsByTagQuery
    with CurrentEventsByTagQuery

  final case class BackoffSettings(
      minBackoff: FiniteDuration,
      maxBackoff: FiniteDuration,
      randomFactor: Double,
      maxRestarts: Int
  )

}

class ThreadReadModelUpdater(
    val readJournal: ReadJournalType,
    val profile: JdbcProfile,
    val db: JdbcProfile#Backend#Database
) extends ThreadComponent
    with ThreadMessageComponent
    with ThreadAdministratorIdsComponent
    with ThreadMemberIdsComponent {

  private val writeRecordsCounter =
    Kamon.metrics.registerCounter("thread-rmu", tags = Map("function" -> "write-records-count"))
  private val readEventsCounter =
    Kamon.metrics.registerCounter("thread-rmu", tags = Map("function" -> "read-events-count"))

  import profile.api._

  def behavior(sqlBatchSize: Long = 1, backoffSettings: Option[BackoffSettings] = None): Behavior[CommandRequest] =
    Behaviors.setup[CommandRequest] { ctx =>
      Behaviors.receiveMessagePartial[CommandRequest] {
        case s: Start =>
          ctx.log.info("RMU start!!")
          ctx.child(s.threadId.value.asString) match {
            case None =>
              ctx.spawn(
                projectionBehavior(sqlBatchSize, backoffSettings, s.threadId),
                name = s"RMU-${s.threadId.value.asString}"
              ) ! s
            case _ =>
              ctx.log.warning("RMU already has started: threadId = {}", s.threadId.value.asString)
          }
          Behaviors.same
      }
    }

  private val logLevels = Attributes.logLevels(
    onElement = Attributes.LogLevels.Info,
    onFailure = Attributes.LogLevels.Error,
    onFinish = Attributes.LogLevels.Info
  )

  type Handler = PartialFunction[
    (Long, ThreadCommonProtocol.Event),
    DBIOAction[Unit, NoStream, Effect.Write with Effect.Transactional]
  ]

  private def forLifecycle(threadId: ThreadId): Handler = {
    case (
        sequenceNr,
        ThreadCreated(_, _, senderId, parentThreadId, title, remarks, administratorIds, memberIds, createdAt)
        ) =>
      DBIO
        .seq(
          insertThreadAction(threadId, sequenceNr, senderId, parentThreadId, title, remarks, createdAt) ::
          insertAdministratorIdsAction(threadId, senderId, administratorIds, createdAt) :::
          insertMemberIdsAction(threadId, senderId, memberIds, createdAt): _*
        ).transactionally
    case (sequenceNr, ThreadDestroyed(_, _, _, createdAt)) =>
      DBIO
        .seq(
          updateThreadToRemoveAction(threadId, sequenceNr, createdAt)
        )
  }

  private def forAdministrator(threadId: ThreadId): Handler = {
    case (sequenceNr, AdministratorIdsJoined(_, _, senderId, administratorIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, sequenceNr, createdAt) ::
          insertAdministratorIdsAction(threadId, senderId, administratorIds, createdAt): _*
        ).transactionally
    case (sequenceNr, AdministratorIdsLeft(_, _, _, administratorIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, sequenceNr, createdAt),
          deleteAdministratorIdsAction(threadId, administratorIds)
        ).transactionally
  }

  private def forMember(threadId: ThreadId): Handler = {
    case (sequenceNr, MemberIdsAdded(_, _, adderId, memberIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, sequenceNr, createdAt) ::
          insertMemberIdsAction(threadId, adderId, memberIds, createdAt): _*
        ).transactionally
    case (sequenceNr, MemberIdsLeft(_, _, _, memberIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, sequenceNr, createdAt),
          deleteMemberIdsAction(threadId, memberIds)
        ).transactionally
  }

  private def forMessage(threadId: ThreadId): Handler = {
    case (sequenceNr, MessagesAdded(_, _, messages, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, sequenceNr, createdAt) ::
          insertMessagesAction(threadId, messages, createdAt): _*
        ).transactionally
    case (sequenceNr, MessagesRemoved(_, _, _, messageIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, sequenceNr, createdAt),
          deleteMessagesAction(messageIds, createdAt)
        ).transactionally
  }

  private def sqlActionFlow(
      threadId: ThreadId
  ): Flow[EventEnvelope, DBIOAction[Unit, NoStream, Effect.Write with Effect.Transactional], NotUsed] =
    Flow[EventEnvelope]
      .map { ee =>
        (ee.sequenceNr, ee.event.asInstanceOf[ThreadCommonProtocol.Event])
      }.map {
        forLifecycle(threadId)
          .orElse(forAdministrator(threadId)).orElse(forMember(threadId)).orElse(forMessage(threadId))
          .orElse {
            case v =>
              DBIO.failed(new Exception(v.toString()))
          }
      }

  private def projectionSource(sqlBatchSize: Long, threadId: ThreadId)(
      implicit ec: ExecutionContext
  ): Source[Vector[Unit], NotUsed] = {
    Source
      .fromFuture(
        db.run(getSequenceNrAction(threadId)).map(_.getOrElse(0L))
      ).log("lastSequenceNr").flatMapConcat { lastSequenceNr =>
        Source
          .repeat(Kamon.tracer.newContext("thread-rmu", token = None, tags = Map("function" -> "read-events-time"))).flatMapConcat {
            tc =>
              readJournal
                .eventsByPersistenceId(ThreadAggregate.name(threadId), lastSequenceNr + 1, Long.MaxValue).map { ee =>
                  tc.finish()
                  ee
                }.recoverWithRetries(attempts = 1, {
                  case ex =>
                    tc.finishWithError(ex)
                    Source.failed[EventEnvelope](ex)
                })
          }
      }.map { events =>
        readEventsCounter.increment
        events
      }.log("ee").via(sqlActionFlow(threadId)).log("sqlAction")
      .batch(sqlBatchSize, ArrayBuffer(_))(_ :+ _).log("batch").mapAsync(1) { sqlActions =>
        val tc = Kamon.tracer.newContext("thread-rmu", token = None, tags = Map("function" -> "write-records-time"))
        Future.successful(tc).flatMap { tc =>
          db.run(DBIO.sequence(sqlActions.result.toVector)).flatMap { result =>
              tc.finish
              Future.successful(result)
            }.recoverWith {
              case ex =>
                tc.finishWithError(ex)
                Future.failed(ex)
            }
        }
      }.map { results =>
        writeRecordsCounter.increment(results.length.toLong)
        results
      }
      .log("run")
      .withAttributes(logLevels)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private def projectionBehavior(
      sqlBatchSize: Long,
      backoffSettings: Option[BackoffSettings],
      threadId: ThreadId
  ): Behavior[Message] =
    Behaviors.setup[Message] { ctx =>
      implicit val system                = ctx.system
      implicit val ec                    = ctx.executionContext
      implicit val mat                   = ActorMaterializer()
      val minBackoff                     = backoffSettings.fold(3 seconds)(_.minBackoff)
      val maxBackoff                     = backoffSettings.fold(30 seconds)(_.maxBackoff)
      val randomFactor                   = backoffSettings.fold(0.2d)(_.randomFactor)
      val maxRestarts                    = backoffSettings.fold(20)(_.maxRestarts)
      var killSwitch: Option[KillSwitch] = None

      Behaviors
        .receiveMessagePartial[Message] {
          case Start(_, tid, _) if tid == threadId =>
            ctx.log.info("projectionBehavior start")
            killSwitch = Some {
              RestartSource
                .withBackoff(minBackoff, maxBackoff, randomFactor, maxRestarts) { () =>
                  projectionSource(sqlBatchSize, tid)
                }.viaMat(
                  KillSwitches.single
                )(
                  Keep.right
                ).toMat(
                  Sink.ignore
                )(Keep.left).withAttributes(logLevels).run()
            }
            Behaviors.same
          case _: Stop =>
            ctx.log.info("projectionBehavior stop")
            Behaviors.stopped
        }.receiveSignal {
          case (ctx, PostStop) =>
            ctx.log.info("post-stop")
            killSwitch.foreach(_.shutdown())
            Behaviors.same
        }
    }

  private def getSequenceNrAction(threadId: ThreadId) = {
    ThreadDao.filter(_.id === threadId.value.asString).map(_.sequenceNr).max.result
  }

  private def updateThreadToRemoveAction(
      threadId: ThreadId,
      sequenceNr: Long,
      createdAt: Instant
  ): FixedSqlAction[Int, NoStream, Effect.Write] = {
    ThreadDao
      .filter(_.id === threadId.value.asString).map { v =>
        (v.sequenceNr, v.removedAt)
      }.update((sequenceNr, Some(createdAt)))
  }

  private def updateSequenceNrInThreadAction(
      threadId: ThreadId,
      sequenceNr: Long,
      createdAt: Instant
  ): FixedSqlAction[Int, NoStream, Effect.Write] = {
    ThreadDao
      .filter(_.id === threadId.value.asString).map { v =>
        (v.sequenceNr, v.updatedAt)
      }.update((sequenceNr, createdAt))
  }

  private def deleteMessagesAction(
      messageIds: MessageIds,
      createdAt: Instant
  ): FixedSqlAction[Int, NoStream, Effect.Write] = {
    ThreadMessageDao
      .filter(_.id.inSet(messageIds.breachEncapsulationOfValues.map(_.value.asString))).map { v =>
        (v.deleted, v.updatedAt)
      }.update(
        (true, createdAt)
      )
  }

  private def insertMessagesAction(
      threadId: ThreadId,
      messages: Messages,
      createdAt: Instant
  ): List[FixedSqlAction[Int, NoStream, Effect.Write]] = {
    messages.breachEncapsulationOfValues.map { message =>
      ThreadMessageDao += ThreadMessageRecordImpl(
        id = message.id.value.asString,
        deleted = false,
        threadId = threadId.value.asString,
        senderId = message.senderId.value.asString,
        `type` = message.`type`,
        body = message.body.toString,
        createdAt = createdAt,
        updatedAt = createdAt
      )
    }.toList
  }

  private def insertMemberIdsAction(
      threadId: ThreadId,
      adderId: AccountId,
      memberIds: MemberIds,
      createdAt: Instant
  ): List[FixedSqlAction[Int, NoStream, Effect.Write]] = {
    memberIds.breachEncapsulationOfValues.map { accountId =>
      ThreadMemberIdsDao += ThreadMemberIdsRecordImpl(
        id = ULID().asString,
        threadId = threadId.value.asString,
        accountId = accountId.value.asString,
        adderId = adderId.value.asString,
        createdAt = createdAt,
        updatedAt = createdAt
      )
    }.toList
  }

  private def deleteMemberIdsAction(
      threadId: ThreadId,
      memberIds: MemberIds
  ): FixedSqlAction[Int, NoStream, Effect.Write] = {
    ThreadMemberIdsDao
      .filter(
        v =>
          v.threadId === threadId.value.asString && v.accountId
            .inSet(memberIds.breachEncapsulationOfValues.map(_.value.asString).toList)
      ).delete
  }

  private def insertAdministratorIdsAction(
      threadId: ThreadId,
      adderId: AccountId,
      administratorIds: AdministratorIds,
      createdAt: Instant
  ): List[FixedSqlAction[Int, NoStream, Effect.Write]] = {
    administratorIds.breachEncapsulationOfValues.map { accountId =>
      ThreadAdministratorIdsDao += ThreadAdministratorIdsRecordImpl(
        id = ULID().asString,
        threadId = threadId.value.asString,
        accountId = accountId.value.asString,
        adderId = adderId.value.asString,
        createdAt = createdAt,
        updatedAt = createdAt
      )
    }.toList
  }

  private def deleteAdministratorIdsAction(
      threadId: ThreadId,
      administratorIds: AdministratorIds
  ): FixedSqlAction[Int, NoStream, Effect.Write] = {
    ThreadAdministratorIdsDao
      .filter(
        v =>
          v.threadId === threadId.value.asString &&
          v.accountId.inSet(administratorIds.breachEncapsulationOfValues.map(_.value.asString).toList)
      ).delete
  }

  private def insertThreadAction(
      threadId: ThreadId,
      sequenceNr: Long,
      senderId: AccountId,
      parentThreadId: Option[ThreadId],
      title: ThreadTitle,
      remarks: Option[ThreadRemarks],
      createdAt: Instant
  ): FixedSqlAction[Int, NoStream, Effect.Write] = {
    ThreadDao += ThreadRecordImpl(
      id = threadId.value.asString,
      deleted = false,
      sequenceNr = sequenceNr,
      creatorId = senderId.value.asString,
      parentId = parentThreadId.map(_.value.asString),
      title = title.value,
      remarks = remarks.map(_.value),
      createdAt = createdAt,
      updatedAt = createdAt,
      removedAt = None
    )
  }
}
