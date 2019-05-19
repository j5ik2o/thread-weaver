package com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed

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
    case (State(None, _), c @ CreateThread(cmdId, threadId, _, _, _, _, _, _, createAt, replyTo)) =>
      Effect.persist(c.toEvent).thenRun { _ =>
        replyTo.foreach(_ ! CreateThreadSucceeded(ULID(), cmdId, threadId, createAt))
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

    // for Administrators
    case (
        State(Some(thread), _),
        c @ JoinAdministratorIds(cmdId, threadId, senderId, administratorIds, createAt, replyTo)
        ) =>
      thread.joinAdministratorIds(administratorIds, senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(
              _ ! JoinAdministratorIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            )
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! JoinAdministratorIdsSucceeded(ULID(), cmdId, threadId, createAt))
          }
      }
    case (
        State(Some(thread), _),
        c @ LeaveAdministratorIds(cmdId, threadId, senderId, administratorIds, createAt, replyTo)
        ) =>
      thread.leaveAdministratorIds(administratorIds, senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(
              _ ! LeaveAdministratorIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            )
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! LeaveAdministratorIdsSucceeded(ULID(), cmdId, threadId, createAt))
          }
      }
    case (State(Some(thread), _), GetAdministratorIds(cmdId, threadId, senderId, createAt, replyTo)) =>
      Effect.none.thenRun { _ =>
        thread.getAdministratorIds(senderId) match {
          case Left(exception) =>
            replyTo ! GetAdministratorIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
          case Right(administratorIds) =>
            replyTo ! GetAdministratorIdsSucceeded(ULID(), cmdId, threadId, administratorIds, createAt)
        }
      }

    // for Members
    case (
        State(Some(thread), _),
        c @ JoinMemberIds(cmdId, threadId, senderId, memberIds, createAt, replyTo)
        ) =>
      thread.joinMemberIds(memberIds, senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(
              _ ! JoinMemberIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            )
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! JoinMemberIdsSucceeded(ULID(), cmdId, threadId, createAt))
          }
      }
    case (
        State(Some(thread), _),
        c @ LeaveMemberIds(cmdId, threadId, senderId, memberIds, createAt, replyTo)
        ) =>
      thread.leaveMemberIds(memberIds, senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(
              _ ! LeaveMemberIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            )
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! LeaveMemberIdsSucceeded(ULID(), cmdId, threadId, createAt))
          }
      }
    case (State(Some(thread), _), GetMemberIds(cmdId, threadId, senderId, createAt, replyTo)) =>
      Effect.none.thenRun { _ =>
        thread.getMemberIds(senderId) match {
          case Left(exception) =>
            replyTo ! GetMemberIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
          case Right(memberIds) =>
            replyTo ! GetMemberIdsSucceeded(ULID(), cmdId, threadId, memberIds, createAt)
        }
      }

    // for Messages
    case (State(Some(thread), _), c @ AddMessages(cmdId, threadId, messages, createAt, replyTo)) =>
      thread.addMessages(messages, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(_ ! AddMessagesFailed(ULID(), cmdId, threadId, exception.getMessage, createAt))
          }
        case Right(_) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(
              _ ! AddMessagesSucceeded(ULID(), cmdId, threadId, messages.toMessageIds, createAt)
            )
          }
      }
    case (State(Some(thread), _), c @ RemoveMessages(cmdId, threadId, senderId, messages, createAt, replyTo)) =>
      thread.removeMessages(messages, senderId, createAt) match {
        case Left(exception) =>
          Effect.none.thenRun { _ =>
            replyTo.foreach(_ ! RemoveMessagesFailed(ULID(), cmdId, threadId, exception.getMessage, createAt))
          }
        case Right((_, messageIds)) =>
          Effect.persist(c.toEvent).thenRun { _ =>
            replyTo.foreach(_ ! RemoveMessagesSucceeded(ULID(), cmdId, threadId, messageIds, createAt))
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
          e.creatorId,
          e.parentThreadId,
          e.title,
          e.remarks,
          e.administratorIds,
          e.memberIds,
          Messages.empty,
          e.createdAt,
          e.createdAt
        )
      )

    // for Administrators
    case (s @ State(Some(thread), _), AdministratorIdsJoined(_, _, adderId, administratorIds, createdAt)) =>
      s.applyState(thread.joinAdministratorIds(administratorIds, adderId, createdAt).right.get)
    case (s @ State(Some(thread), _), AdministratorIdsLeft(_, _, removerId, administratorIds, createdAt)) =>
      s.applyState(thread.leaveAdministratorIds(administratorIds, removerId, createdAt).right.get)

    // for Members
    case (s @ State(Some(thread), _), MemberIdsAdded(_, _, adderId, memberIds, createdAt)) =>
      s.applyState(thread.joinMemberIds(memberIds, adderId, createdAt).right.get)
    case (s @ State(Some(thread), _), MemberIdsLeft(_, _, removerId, memberIds, createdAt)) =>
      s.applyState(thread.leaveMemberIds(memberIds, removerId, createdAt).right.get)

    // for Messages
    case (s @ State(Some(thread), _), MessagesAdded(_, _, messages, createdAt)) =>
      s.applyState(thread.addMessages(messages, createdAt).right.get)
    case (s @ State(Some(thread), _), MessagesRemoved(_, _, removerId, messages, createdAt)) =>
      s.applyState(thread.removeMessages(messages, removerId, createdAt).right.get._1)

    case (s @ State(Some(thread), _), ThreadDestroyed(_, _, destroyerId, createdAt)) =>
      s.applyState(thread.destroy(destroyerId, createdAt).right.get)

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
