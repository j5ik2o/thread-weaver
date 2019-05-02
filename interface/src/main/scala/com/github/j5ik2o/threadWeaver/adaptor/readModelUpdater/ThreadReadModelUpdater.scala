package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.stream.typed.scaladsl.ActorMaterializer
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.Event
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object ThreadReadModelUpdater {

  trait Message

  trait CommandMessage extends Message {
    def id: ULID
    def threadId: ThreadId
    def createAt: Instant
  }
  trait CommandRequest  extends CommandMessage
  trait CommandResponse extends CommandMessage

  private case class EventMessage(persistenceId: String, sequenceNr: Long, event: Event)

  def behavior(threadId: ThreadId): Behavior[ThreadId] = Behaviors.setup[ThreadId] { ctx =>
    Behaviors.receiveMessagePartial[ThreadId] {
      case threadId: ThreadId =>
        ctx.child(threadId.value.asString) match {
          case None =>
            ctx.spawn(child(threadId), name = s"RMU-${threadId.value.asString}")
          case Some(childRef) =>
            childRef.asInstanceOf[ActorRef[ThreadId]]
        }
        Behaviors.same
    }
    Behaviors.same
  }

  private def child(threadId: ThreadId): Behavior[EventMessage] = Behaviors.setup[EventMessage] { ctx =>
    implicit val system = ctx.system
    implicit val mat    = ActorMaterializer()
    val q: LeveldbReadJournal =
      PersistenceQuery(ctx.system.toUntyped).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
    q.eventsByPersistenceId(threadId.value.asString, 0, Long.MaxValue).runForeach { ee =>
      ctx.self ! EventMessage(ee.persistenceId, ee.sequenceNr, ee.event.asInstanceOf[Event])
    }
    Behaviors.receiveMessagePartial[EventMessage] {
      case _ =>
        // TODO
        Behaviors.same
    }
  }

}
