package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ Behavior, PostStop }
import akka.persistence.query.scaladsl._
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.stream.{ KillSwitch, KillSwitches }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.{ Event, MemberIdsAdded, ThreadCreated }
import com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc.{
  ThreadAdministratorIdsComponent,
  ThreadComponent,
  ThreadMemberIdsComponent,
  ThreadMessageComponent
}
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ AdministratorIds, MemberIds, ThreadId }
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
    Behaviors.same
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
              case MemberIdsAdded(_, _, adderId, memberIds, _) =>
              case _                                           =>
              // TODO
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
                }.viaMat(
                  KillSwitches.single
                )(
                  Keep.right
                ).toMat(Sink.foreach { ee =>
                  ctx.self ! EventMessage(ee.persistenceId, ee.sequenceNr, ee.asInstanceOf[Event])
                })(Keep.left).run()
            )
            Behaviors.same
          case _: Stop =>
            Behaviors.stopped
        }.receiveSignal {
          case (_, PostStop) =>
            killSwitch.foreach(_.shutdown())
            Behaviors.same
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
