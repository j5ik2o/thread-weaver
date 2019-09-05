package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.NotUsed
import akka.actor.{ Actor, ActorLogging, Props }
import akka.persistence.query.EventEnvelope
import akka.persistence.query.scaladsl._
import akka.stream.scaladsl.{ Flow, Keep, RestartSource, Sink, Source }
import akka.stream.{ ActorMaterializer, Attributes, KillSwitch, KillSwitches }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadCommonProtocol
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

  def props(
      readJournal: ReadJournalType,
      profile: JdbcProfile,
      db: JdbcProfile#Backend#Database,
      sqlBatchSize: Long = 1,
      backoffSettings: Option[BackoffSettings] = None
  ): Props = Props(new ThreadReadModelUpdater(readJournal, profile, db, sqlBatchSize, backoffSettings))

}

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class ThreadReadModelUpdater(
    val readJournal: ReadJournalType,
    val profile: JdbcProfile,
    val db: JdbcProfile#Backend#Database,
    sqlBatchSize: Long = 1,
    backoffSettings: Option[BackoffSettings] = None
) extends Actor
    with ActorLogging
    with ThreadComponent
    with ThreadMessageComponent
    with ThreadAdministratorIdsComponent
    with ThreadMemberIdsComponent {

  private val writeRecordsCounter =
    Kamon.metrics.registerCounter("thread-rmu", tags = Map("function" -> "write-records-count"))
  private val readEventsCounter =
    Kamon.metrics.registerCounter("thread-rmu", tags = Map("function" -> "read-events-count"))

  import profile.api._

  val minBackoff   = backoffSettings.fold(3 seconds)(_.minBackoff)
  val maxBackoff   = backoffSettings.fold(30 seconds)(_.maxBackoff)
  val randomFactor = backoffSettings.fold(0.2d)(_.randomFactor)
  val maxRestarts  = backoffSettings.fold(20)(_.maxRestarts)

  import context.dispatcher
  implicit val system                = context.system
  implicit val mat                   = ActorMaterializer()
  var killSwitch: Option[KillSwitch] = None

  private def startStream(threadTag: ThreadTag): Unit = {
    killSwitch = Some {
      RestartSource
        .withBackoff(minBackoff, maxBackoff, randomFactor, maxRestarts) { () =>
          projectionSource(sqlBatchSize, threadTag)
        }.viaMat(
          KillSwitches.single
        )(
          Keep.right
        ).toMat(
          Sink.ignore
        )(Keep.left).withAttributes(logLevels).run()
    }
  }

  def receive: Receive = {
    case s: Start =>
      log.info("RMU start!!")
      implicit val config = context.system.settings.config
      if (killSwitch.isEmpty)
        startStream(s.threadTag)
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

  private def forLifecycle(threadTag: ThreadTag): Handler = {
    case (
        sequenceNr,
        ThreadCreated(_, threadId, senderId, parentThreadId, title, remarks, administratorIds, memberIds, createdAt)
        ) =>
      DBIO
        .seq(
          insertThreadAction(threadId, threadTag, sequenceNr, senderId, parentThreadId, title, remarks, createdAt) ::
          insertAdministratorIdsAction(threadId, senderId, administratorIds, createdAt) :::
          insertMemberIdsAction(threadId, senderId, memberIds, createdAt): _*
        ).transactionally
    case (sequenceNr, ThreadDestroyed(_, threadId, _, createdAt)) =>
      DBIO
        .seq(
          updateThreadToRemoveAction(threadId, sequenceNr, createdAt)
        )
  }

  private def forAdministrator(threadTag: ThreadTag): Handler = {
    case (sequenceNr, AdministratorIdsJoined(_, threadId, senderId, administratorIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, threadTag, sequenceNr, createdAt) ::
          insertAdministratorIdsAction(threadId, senderId, administratorIds, createdAt): _*
        ).transactionally
    case (sequenceNr, AdministratorIdsLeft(_, threadId, _, administratorIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, threadTag, sequenceNr, createdAt),
          deleteAdministratorIdsAction(threadId, administratorIds)
        ).transactionally
  }

  private def forMember(threadTag: ThreadTag): Handler = {
    case (sequenceNr, MemberIdsAdded(_, threadId, adderId, memberIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, threadTag, sequenceNr, createdAt) ::
          insertMemberIdsAction(threadId, adderId, memberIds, createdAt): _*
        ).transactionally
    case (sequenceNr, MemberIdsLeft(_, threadId, _, memberIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, threadTag, sequenceNr, createdAt),
          deleteMemberIdsAction(threadId, memberIds)
        ).transactionally
  }

  private def forMessage(threadTag: ThreadTag): Handler = {
    case (sequenceNr, MessagesAdded(_, threadId, messages, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, threadTag, sequenceNr, createdAt) ::
          insertMessagesAction(threadId, messages, createdAt): _*
        ).transactionally
    case (sequenceNr, MessagesRemoved(_, threadId, _, messageIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThreadAction(threadId, threadTag, sequenceNr, createdAt),
          deleteMessagesAction(messageIds, createdAt)
        ).transactionally
  }

  private def sqlActionFlow(
      threadTag: ThreadTag
  ): Flow[EventEnvelope, DBIOAction[Unit, NoStream, Effect.Write with Effect.Transactional], NotUsed] =
    Flow[EventEnvelope]
      .map { ee =>
        (ee.sequenceNr, ee.event.asInstanceOf[ThreadCommonProtocol.Event])
      }.map {
        forLifecycle(threadTag)
          .orElse(forAdministrator(threadTag)).orElse(forMember(threadTag)).orElse(forMessage(threadTag))
          .orElse {
            case v =>
              DBIO.failed(new Exception(v.toString()))
          }
      }

  private def projectionSource(sqlBatchSize: Long, threadTag: ThreadTag)(
      implicit ec: ExecutionContext
  ): Source[Vector[Unit], NotUsed] = {
    Source
      .fromFuture(
        db.run(getSequenceNrAction(threadTag)).map(_.getOrElse(0L))
      ).log("lastSequenceNr").flatMapConcat { lastSequenceNr =>
        Source
          .repeat(Kamon.tracer.newContext("thread-rmu", token = None, tags = Map("function" -> "read-events-time"))).flatMapConcat {
            tc =>
              readJournal
                .eventsByTag(threadTag.value, akka.persistence.query.Sequence(lastSequenceNr)).log("events-by-tag").map {
                  ee =>
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
      }.log("ee").via(sqlActionFlow(threadTag)).log("sqlAction")
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

  private def getSequenceNrAction(threadTag: ThreadTag) = {
    ThreadDao.filter(_.persistenceTag === threadTag.value).map(_.sequenceNr).max.result
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
      threadTag: ThreadTag,
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
      threadTag: ThreadTag,
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
      persistenceTag = threadTag.value,
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
