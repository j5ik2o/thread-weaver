package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior, PostStop }
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.persistence.typed.{ PersistenceId, RecoveryCompleted }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ Messages, Thread, ThreadId }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object PersistentThreadAggregate {

  private val commandHandler: (State, CommandRequest) => Effect[Event, State] = {
    case (State(None, _), c @ CreateThread(cmdId, threadId, _, _, _, _, createAt, replyTo)) =>
      Effect.persist(c.toEvent).thenRun { _ =>
        replyTo.foreach(_ ! CreateThreadSucceeded(ULID(), cmdId, threadId, createAt))
      }

    case (
        State(Some(thread), _),
        c @ AddAdministratorIds(cmdId, threadId, senderId, administratorIds, createAt, replyTo)
        ) =>
      thread.addAdministratorIds(administratorIds, senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(
              _ ! AddAdministratorIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            )
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! AddAdministratorIdsSucceeded(ULID(), cmdId, threadId, createAt))
          }
      }

    case (
        State(Some(thread), _),
        c @ RemoveAdministratorIds(cmdId, threadId, senderId, administratorIds, createAt, replyTo)
        ) =>
      thread.removeAdministratorIds(administratorIds, senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(
              _ ! RemoveAdministratorIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            )
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! RemoveAdministratorIdsSucceeded(ULID(), cmdId, threadId, createAt))
          }
      }

    case (
        State(Some(thread), _),
        c @ AddMemberIds(cmdId, threadId, senderId, memberIds, createAt, replyTo)
        ) =>
      thread.addMemberIds(memberIds, senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(
              _ ! AddMemberIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            )
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! AddMemberIdsSucceeded(ULID(), cmdId, threadId, createAt))
          }

      }
    case (
        State(Some(thread), _),
        c @ RemoveMemberIds(cmdId, threadId, senderId, memberIds, createAt, replyTo)
        ) =>
      thread.removeMemberIds(memberIds, senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(
              _ ! RemoveMemberIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            )
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! RemoveMemberIdsSucceeded(ULID(), cmdId, threadId, createAt))
          }

      }

    case (State(Some(thread), _), c @ AddMessages(cmdId, threadId, senderId, messages, createAt, replyTo)) =>
      thread.addMessages(messages, senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(_ ! AddMessagesFailed(ULID(), cmdId, threadId, exception.getMessage, createAt))
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! AddMessagesSucceeded(ULID(), cmdId, threadId, createAt))
          }
      }
    case (State(Some(thread), _), c @ RemoveMessages(cmdId, threadId, senderId, messages, createAt, replyTo)) =>
      thread.removeMessages(messages, senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(_ ! RemoveMessagesFailed(ULID(), cmdId, threadId, exception.getMessage, createAt))
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! RemoveMessagesSucceeded(ULID(), cmdId, threadId, createAt))
          }
      }

    case (State(Some(thread), _), c @ DestroyThread(cmdId, threadId, senderId, createAt, replyTo)) =>
      thread.destroy(senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(
              _ ! DestroyThreadFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            )
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! DestroyThreadSucceeded(ULID(), cmdId, threadId, createAt))
          }
      }

    case (State(Some(thread), _), GetMessages(cmdId, threadId, senderId, createAt, replyTo)) =>
      Effect.none.thenRun { _ =>
        thread.getMessages(senderId) match {
          case Left(exception) =>
            replyTo ! GetMessagesFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
          case Right(messages) =>
            replyTo ! GetMessagesSucceeded(ULID(), cmdId, threadId, messages, createAt)
        }
      }

    case _ =>
      Effect.none

  }

  private val eventHandler: (State, Event) => State = {
    case (s @ State(None, _), e: ThreadCreated) =>
      s.applyState(
        Thread(
          e.threadId,
          e.senderId,
          e.parentThreadId,
          e.administratorIds,
          e.memberIds,
          Messages.empty,
          e.createdAt,
          e.createdAt
        )
      )

    // for Administrators
    case (s @ State(Some(thread), _), AdministratorIdsAdded(_, _, senderId, administratorIds, createdAt)) =>
      s.applyState(thread.addAdministratorIds(administratorIds, senderId, createdAt).right.get)
    case (s @ State(Some(thread), _), AdministratorIdsRemoved(_, _, senderId, administratorIds, createdAt)) =>
      s.applyState(thread.removeAdministratorIds(administratorIds, senderId, createdAt).right.get)

    // for Members
    case (s @ State(Some(thread), _), MemberIdsAdded(_, _, senderId, memberIds, createdAt)) =>
      s.applyState(thread.addMemberIds(memberIds, senderId, createdAt).right.get)
    case (s @ State(Some(thread), _), MemberIdsRemoved(_, _, senderId, memberIds, createdAt)) =>
      s.applyState(thread.removeMemberIds(memberIds, senderId, createdAt).right.get)

    // for Messages
    case (s @ State(Some(thread), _), MessagesAdded(_, _, senderId, messages, createdAt)) =>
      s.applyState(thread.addMessages(messages, senderId, createdAt).right.get)
    case (s @ State(Some(thread), _), MessagesRemoved(_, _, senderId, messages, createdAt)) =>
      s.applyState(thread.removeMessages(messages, senderId, createdAt).right.get)

    case (s @ State(Some(thread), _), e: ThreadDestroyed) =>
      s.applyState(thread.destroy(e.senderId, e.createdAt).right.get)

    case (state, _) =>
      state

  }

  def behavior(id: ThreadId, subscribers: Seq[ActorRef[Message]]): Behavior[CommandRequest] =
    Behaviors.setup[CommandRequest] { ctx =>
      subscribers.foreach(_ ! Started(ULID(), id, Instant.now, ctx.self))

      EventSourcedBehavior[CommandRequest, Event, State](
        persistenceId = PersistenceId(id.value.asString),
        emptyState = State(None, subscribers),
        commandHandler,
        eventHandler
      ).receiveSignal {
        case (_, PostStop) =>
          subscribers.foreach(_ ! Stopped(ULID(), id, Instant.now, ctx.self))
        case (_, RecoveryCompleted) =>
          ctx.log.info("recovery completed")

      }
    }

  case class State(thread: Option[Thread], subscribers: Seq[ActorRef[Message]]) {
    def applyState(value: Thread): State = copy(thread = Some(value))
  }

}
