package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ Behavior, PostStop }
import akka.persistence.query.scaladsl._
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.stream.{ KillSwitch, KillSwitches }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.{ Event, ThreadCreated }
import com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc.{
  ThreadAdministratorIdsComponent,
  ThreadComponent,
  ThreadMemberIdsComponent,
  ThreadMessageComponent
}
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import slick.jdbc.JdbcProfile

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
              case ThreadCreated(_, _, parentThreadId, administratorIds, memberIds, createdAt) =>
                val insertThread = ThreadDao += ThreadRecord(
                      threadId.value.asString,
                      deleted = false,
                      sequenceNr,
                      parentThreadId.map(_.value.asString),
                      createdAt,
                      createdAt
                    )
                val insertAdministratorIds = administratorIds.breachEncapsulationOfValues
                  .map(
                    aid =>
                      ThreadAdministratorIdsDao += ThreadAdministratorIdsRecord(
                          ULID().asString,
                          threadId.value.asString,
                          aid.value.asString,
                          createdAt,
                          createdAt
                        )
                  ).toList
                val insertMemberIds = memberIds.breachEncapsulationOfValues
                  .map(
                    aid =>
                      ThreadMemberIdsDao += ThreadMemberIdsRecord(
                          ULID().asString,
                          threadId.value.asString,
                          aid.value.asString,
                          createdAt,
                          createdAt
                        )
                  )
                db.run(
                  DBIO
                    .seq(insertThread :: insertAdministratorIds ++ insertMemberIds: _*).transactionally
                )
              case _ =>
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

}
