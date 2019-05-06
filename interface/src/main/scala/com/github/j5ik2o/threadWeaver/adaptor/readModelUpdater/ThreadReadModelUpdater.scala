package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ Behavior, PostStop }
import akka.persistence.query.scaladsl._
import akka.stream.scaladsl.{ Keep, RestartSource, Sink, Source }
import akka.stream.typed.scaladsl.ActorMaterializer
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
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.{
  BackoffSettings,
  ReadJournalType
}
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ Message => _, _ }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import slick.jdbc.JdbcProfile
import slick.sql.FixedSqlAction

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

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
    val db: JdbcProfile#Backend#Database,
    batchSize: Long = 10,
    backoffSettings: Option[BackoffSettings] = None
) extends ThreadComponent
    with ThreadMessageComponent
    with ThreadAdministratorIdsComponent
    with ThreadMemberIdsComponent {

  import profile.api._

  private val minBackoff   = backoffSettings.fold(3 seconds)(_.minBackoff)
  private val maxBackoff   = backoffSettings.fold(30 seconds)(_.maxBackoff)
  private val randomFactor = backoffSettings.fold(0.2d)(_.randomFactor)
  private val maxRestarts  = backoffSettings.fold(20)(_.maxRestarts)

  def behavior: Behavior[CommandRequest] = Behaviors.setup[CommandRequest] { ctx =>
    Behaviors.receiveMessagePartial[CommandRequest] {
      case s: Start =>
        ctx.child(s.threadId.value.asString) match {
          case None =>
            ctx.spawn(projectionBehavior(s.threadId), name = s"RMU-${s.threadId.value.asString}") ! s
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

  private def projectionHandler(
      threadId: ThreadId
  ): (Long, Event) => DBIOAction[Unit, NoStream, Effect.Write with Effect.Transactional] = {
    case (sequenceNr, ThreadCreated(_, _, senderId, parentThreadId, administratorIds, memberIds, createdAt)) =>
      DBIO
        .seq(
          insertThread(threadId, sequenceNr, senderId, parentThreadId, createdAt) ::
          insertAdministratorIds(threadId, senderId, administratorIds, createdAt) :::
          insertMemberIds(threadId, senderId, memberIds, createdAt): _*
        ).transactionally
    case (sequenceNr, ThreadDestroyed(_, _, _, createdAt)) =>
      DBIO
        .seq(
          updateThreadToRemove(threadId, sequenceNr, createdAt)
        )
    case (sequenceNr, AdministratorIdsAdded(_, _, senderId, administratorIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThread(threadId, sequenceNr, createdAt) ::
          insertAdministratorIds(threadId, senderId, administratorIds, createdAt): _*
        ).transactionally
    case (sequenceNr, MemberIdsAdded(_, _, adderId, memberIds, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThread(threadId, sequenceNr, createdAt) ::
          insertMemberIds(threadId, adderId, memberIds, createdAt): _*
        ).transactionally
    case (sequenceNr, MessagesAdded(_, _, senderId, messages, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThread(threadId, sequenceNr, createdAt) ::
          insertMessages(threadId, senderId, messages, createdAt): _*
        ).transactionally
    case (sequenceNr, MessagesRemoved(_, _, messageIds, _, createdAt)) =>
      DBIO
        .seq(
          updateSequenceNrInThread(threadId, sequenceNr, createdAt),
          deleteMessages(messageIds, createdAt)
        ).transactionally
    case _ =>
      DBIO.successful(())
  }

  private def projectionSource(threadId: ThreadId)(implicit ec: ExecutionContext) = {
    Source
      .fromFuture(
        db.run(ThreadDao.filter(_.id === threadId.value.asString).map(_.sequenceNr).max.result).map(
            _.getOrElse(0L)
          )
      ).flatMapConcat { lastSequenceNr =>
        readJournal
          .eventsByPersistenceId(threadId.value.asString, lastSequenceNr + 1, Long.MaxValue)
      }.map { ee =>
        projectionHandler(threadId)(ee.sequenceNr, ee.event.asInstanceOf[Event])
      }.batch(batchSize, ArrayBuffer(_))(_ :+ _).mapAsync(1) { v =>
        db.run(DBIO.sequence(v.result.toVector))
      }.withAttributes(logLevels)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private def projectionBehavior(threadId: ThreadId): Behavior[Message] =
    Behaviors.setup[Message] { ctx =>
      implicit val system                = ctx.system
      implicit val ec                    = ctx.executionContext
      implicit val mat                   = ActorMaterializer()
      var killSwitch: Option[KillSwitch] = None

      Behaviors
        .receiveMessagePartial[Message] {
          case Start(_, tid, _) if tid == threadId =>
            killSwitch = Some {
              RestartSource
                .withBackoff(minBackoff, maxBackoff, randomFactor, maxRestarts) { () =>
                  projectionSource(tid)
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
            Behaviors.stopped
        }.receiveSignal {
          case (_, PostStop) =>
            killSwitch.foreach(_.shutdown())
            Behaviors.same
        }
    }

  private def updateThreadToRemove(
      threadId: ThreadId,
      sequenceNr: Long,
      createdAt: Instant
  ): FixedSqlAction[Int, NoStream, Effect.Write] = {
    ThreadDao
      .filter(_.id === threadId.value.asString).map { v =>
        (v.sequenceNr, v.removedAt)
      }.update((sequenceNr, Some(createdAt)))
  }

  private def updateSequenceNrInThread(
      threadId: ThreadId,
      sequenceNr: Long,
      createdAt: Instant
  ): FixedSqlAction[Int, NoStream, Effect.Write] = {
    ThreadDao
      .filter(_.id === threadId.value.asString).map { v =>
        (v.sequenceNr, v.updatedAt)
      }.update((sequenceNr, createdAt))
  }

  private def deleteMessages(
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

  private def insertMessages(
      threadId: ThreadId,
      senderId: AccountId,
      messages: Messages,
      createdAt: Instant
  ): List[FixedSqlAction[Int, NoStream, Effect.Write]] = {
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
    }.toList
  }

  private def insertMemberIds(
      threadId: ThreadId,
      adderId: AccountId,
      memberIds: MemberIds,
      createdAt: Instant
  ): List[FixedSqlAction[Int, NoStream, Effect.Write]] = {
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

  private def insertAdministratorIds(
      threadId: ThreadId,
      adderId: AccountId,
      administratorIds: AdministratorIds,
      createdAt: Instant
  ): List[FixedSqlAction[Int, NoStream, Effect.Write]] = {
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

  private def insertThread(
      threadId: ThreadId,
      sequenceNr: Long,
      senderId: AccountId,
      parentThreadId: Option[ThreadId],
      createdAt: Instant
  ): FixedSqlAction[Int, NoStream, Effect.Write] = {
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
