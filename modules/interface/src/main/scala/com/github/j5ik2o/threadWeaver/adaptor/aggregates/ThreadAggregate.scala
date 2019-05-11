package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import akka.actor.typed.{ ActorRef, Behavior, PostStop }
import akka.actor.typed.scaladsl.{ AbstractBehavior, ActorContext, Behaviors }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ Messages, Thread, ThreadId }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

class ThreadAggregate(context: ActorContext[CommandRequest])(id: ThreadId, subscribers: Seq[ActorRef[Message]])
    extends AbstractBehavior[CommandRequest] {

  subscribers.foreach(_ ! Started(ULID(), id, Instant.now, context.self))

  override def onMessage(msg: CommandRequest): Behavior[CommandRequest] = onStarted.receiveSignal {
    case (_, PostStop) =>
      subscribers.foreach(_ ! Stopped(ULID(), id, Instant.now, context.self))
      Behaviors.same
  }

  private def onDestroyed: Behavior[CommandRequest] = Behaviors.unhandled[CommandRequest]

  private def commandAdministratorIds(thread: Thread): Behaviors.Receive[CommandRequest] =
    Behaviors.receiveMessagePartial[CommandRequest] {
      case JoinAdministratorIds(cmdId, threadId, senderId, administratorIds, createAt, replyTo) if threadId == id =>
        thread.joinAdministratorIds(administratorIds, senderId, createAt) match {
          case Left(exception) =>
            replyTo.foreach(
              _ ! JoinAdministratorIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            )
            Behaviors.same
          case Right(newThread) =>
            replyTo.foreach(_ ! JoinAdministratorIdsSucceeded(ULID(), cmdId, threadId, createAt))
            onCreated(newThread)
        }
      case LeaveAdministratorIds(cmdId, threadId, senderId, administratorIds, createAt, replyTo) if threadId == id =>
        thread.leaveAdministratorIds(administratorIds, senderId, createAt) match {
          case Left(exception) =>
            replyTo.foreach(
              _ ! LeaveAdministratorIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            )
            Behaviors.same
          case Right(newThread) =>
            replyTo.foreach(_ ! LeaveAdministratorIdsSucceeded(ULID(), cmdId, threadId, createAt))
            onCreated(newThread)
        }
    }

  private def queryAdministratorIds(thread: Thread): Behaviors.Receive[CommandRequest] =
    Behaviors.receiveMessagePartial[CommandRequest] {
      // for Administrators
      case GetAdministratorIds(cmdId, threadId, senderId, createAt, replyTo) if threadId == id =>
        thread.getAdministratorIds(senderId) match {
          case Left(exception) =>
            replyTo ! GetAdministratorIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            Behaviors.same
          case Right(administratorIds) =>
            replyTo ! GetAdministratorIdsSucceeded(ULID(), cmdId, threadId, administratorIds, createAt)
            Behaviors.same
        }
    }

  private def commandMemberIds(thread: Thread): Behaviors.Receive[CommandRequest] =
    Behaviors.receiveMessagePartial[CommandRequest] {
      // for Members
      case JoinMemberIds(requestId, threadId, senderId, memberIds, createAt, replyTo) if threadId == id =>
        thread.joinMemberIds(memberIds, senderId, createAt) match {
          case Left(exception) =>
            replyTo.foreach(
              _ ! JoinMemberIdsFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
            )
            Behaviors.same
          case Right(newThread) =>
            replyTo.foreach(_ ! JoinMemberIdsSucceeded(ULID(), requestId, threadId, createAt))
            onCreated(newThread)
        }
      case LeaveMemberIds(requestId, threadId, senderId, memberIds, createAt, replyTo) if threadId == id =>
        thread.joinMemberIds(memberIds, senderId, createAt) match {
          case Left(exception) =>
            replyTo.foreach(
              _ ! LeaveMemberIdsFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
            )
            Behaviors.same
          case Right(newThread) =>
            replyTo.foreach(_ ! LeaveMemberIdsSucceeded(ULID(), requestId, threadId, createAt))
            onCreated(newThread)
        }
    }

  private def queryMemberIds(thread: Thread): Behaviors.Receive[CommandRequest] =
    Behaviors.receiveMessagePartial[CommandRequest] {
      case GetMemberIds(cmdId, threadId, senderId, createAt, replyTo) =>
        thread.getMemberIds(senderId) match {
          case Left(exception) =>
            replyTo ! GetMemberIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
            Behaviors.same
          case Right(memberIds) =>
            replyTo ! GetMemberIdsSucceeded(ULID(), cmdId, threadId, memberIds, createAt)
            Behaviors.same
        }
    }

  private def commandMessages(thread: Thread): Behaviors.Receive[CommandRequest] =
    Behaviors.receiveMessagePartial[CommandRequest] {
      case AddMessages(requestId, threadId, messages, createAt, replyTo) if threadId == id =>
        thread.addMessages(messages, createAt) match {
          case Left(exception) =>
            replyTo.foreach(_ ! AddMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt))
            Behaviors.same
          case Right(newThread) =>
            replyTo.foreach(_ ! AddMessagesSucceeded(ULID(), requestId, threadId, messages.toMessageIds, createAt))
            onCreated(newThread)
        }
      case RemoveMessages(requestId, threadId, senderId, messageIds, createAt, replyTo) if threadId == id =>
        thread.removeMessages(messageIds, senderId, createAt) match {
          case Left(exception) =>
            replyTo.foreach(_ ! RemoveMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt))
            Behaviors.same
          case Right(newThread) =>
            replyTo.foreach(_ ! RemoveMessagesSucceeded(ULID(), requestId, threadId, createAt))
            onCreated(newThread)
        }
    }

  private def queryMessages(thread: Thread): Behaviors.Receive[CommandRequest] =
    Behaviors.receiveMessagePartial[CommandRequest] {
      case GetMessages(cmdId, threadId, senderId, createAt, replyTo) if threadId == id =>
        thread.getMessages(senderId) match {
          case Left(exception) =>
            replyTo ! GetMessagesFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
          case Right(messages) =>
            replyTo ! GetMessagesSucceeded(ULID(), cmdId, threadId, messages, createAt)
        }
        Behaviors.same
    }

  private def destroy(thread: Thread): Behaviors.Receive[CommandRequest] =
    Behaviors.receiveMessagePartial[CommandRequest] {
      case DestroyThread(requestId, threadId, senderId, createAt, replyTo) if threadId == id =>
        thread.destroy(senderId, createAt) match {
          case Left(exception) =>
            replyTo.foreach(_ ! DestroyThreadFailed(ULID(), requestId, threadId, exception.getMessage, createAt))
            Behaviors.same
          case Right(_) =>
            replyTo.foreach(_ ! DestroyThreadSucceeded(ULID(), requestId, threadId, createAt))
            onDestroyed
        }
    }

  private def create: Behaviors.Receive[CommandRequest] = {
    Behaviors.receiveMessagePartial[CommandRequest] {
      case CreateThread(requestId, threadId, _, _, _, _, _, _, createAt, replyTo) if threadId == id =>
        replyTo.foreach(_ ! CreateThreadFailed(ULID(), requestId, threadId, "Already created", createAt))
        Behaviors.same
    }
  }

  private def onCreated(thread: Thread): Behavior[CommandRequest] =
    create
      .orElse(destroy(thread))
      .orElse(commandAdministratorIds(thread).orElse(queryAdministratorIds(thread)))
      .orElse(commandMemberIds(thread).orElse(queryMemberIds(thread)))
      .orElse(commandMessages(thread).orElse(queryMessages(thread)))

  private def onStarted: Behaviors.Receive[CommandRequest] = Behaviors.receiveMessage[CommandRequest] {
    case CreateThread(
        requestId,
        threadId,
        creatorId,
        parentThreadId,
        title,
        remarks,
        administratorIds,
        memberIds,
        createAt,
        replyTo
        ) if threadId == id =>
      replyTo.foreach(_ ! CreateThreadSucceeded(ULID(), requestId, threadId, createAt))
      onCreated(
        Thread(
          threadId,
          creatorId,
          parentThreadId,
          title,
          remarks,
          administratorIds,
          memberIds,
          Messages.empty,
          createAt,
          createAt
        )
      )

    case DestroyThread(requestId, threadId, _, createAt, replyTo) if threadId == id =>
      replyTo.foreach(_ ! DestroyThreadFailed(ULID(), requestId, threadId, "Not created yet", createAt))
      Behaviors.same

    case AddMessages(requestId, threadId, _, createAt, replyTo) if threadId == id =>
      replyTo.foreach(_ ! AddMessagesFailed(ULID(), requestId, threadId, "Not created yet", createAt))
      Behaviors.same

    case RemoveMessages(requestId, threadId, _, _, createAt, replyTo) if threadId == id =>
      replyTo.foreach(_ ! RemoveMessagesFailed(ULID(), requestId, threadId, "Not created yet", createAt))
      Behaviors.same
  }

}

object ThreadAggregate {

  def name(id: ThreadId): String = s"thread-${id.value.asString}"

  def behavior(id: ThreadId, subscribers: Seq[ActorRef[Message]]): Behavior[CommandRequest] =
    Behaviors.setup[CommandRequest] { ctx =>
      new ThreadAggregate(ctx)(id, subscribers)
    }

}
