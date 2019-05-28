package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import java.time.Instant

import akka.actor.{ Actor, ActorRef, Props }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadCommonProtocol.{ Started, Stopped }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ Messages, Thread, ThreadId }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object ThreadAggregate {

  def name(id: ThreadId): String = s"thread-${id.value.asString}"

  def props(id: ThreadId, subscribers: Seq[ActorRef]): Props = Props(new ThreadAggregate(id, subscribers))

}

class ThreadAggregate(id: ThreadId, subscribers: Seq[ActorRef]) extends Actor {

  private def onDestroyed: Receive = {
    case ExistsThread(requestId, threadId, senderId, createAt) if threadId == id =>
      sender() ! ExistsThreadSucceeded(ULID(), requestId, threadId, exists = false, createAt)
  }

  private def commandJoinMemberIds(thread: Thread): Receive = {
    case JoinMemberIds(requestId, threadId, senderId, memberIds, createAt, replyTo) if threadId == id =>
      thread.joinMemberIds(memberIds, senderId, createAt) match {
        case Left(exception) =>
          if (replyTo)
            sender() ! JoinMemberIdsFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
        case Right(newThread) =>
          if (replyTo)
            sender() ! JoinMemberIdsSucceeded(ULID(), requestId, threadId, createAt)
          context.become(onCreated(newThread))
      }
  }

  private def commandLeaveMemberIds(thread: Thread): Receive = {
    case LeaveMemberIds(requestId, threadId, senderId, memberIds, createAt, replyTo) if threadId == id =>
      thread.joinMemberIds(memberIds, senderId, createAt) match {
        case Left(exception) =>
          if (replyTo)
            sender() ! LeaveMemberIdsFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
        case Right(newThread) =>
          if (replyTo)
            sender() ! LeaveMemberIdsSucceeded(ULID(), requestId, threadId, createAt)
          context.become(onCreated(newThread))
      }
  }

  private def queryMemberIds(thread: Thread): Receive = {
    case GetMemberIds(cmdId, threadId, senderId, createAt) =>
      thread.getMemberIds(senderId) match {
        case Left(exception) =>
          sender() ! GetMemberIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
        case Right(memberIds) =>
          sender() ! GetMemberIdsSucceeded(ULID(), cmdId, threadId, memberIds, createAt)
      }
  }

  private def commandJoinAdministratorIds(thread: Thread): Receive = {
    case JoinAdministratorIds(cmdId, threadId, senderId, administratorIds, createAt, replyTo) if threadId == id =>
      thread.joinAdministratorIds(administratorIds, senderId, createAt) match {
        case Left(exception) =>
          if (replyTo)
            sender() ! JoinAdministratorIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
        case Right(newThread) =>
          if (replyTo)
            sender() ! JoinAdministratorIdsSucceeded(ULID(), cmdId, threadId, createAt)
          context.become(onCreated(newThread))
      }
  }

  private def commandLeaveAdministratorIds(thread: Thread): Receive = {
    case LeaveAdministratorIds(cmdId, threadId, senderId, administratorIds, createAt, replyTo) if threadId == id =>
      thread.leaveAdministratorIds(administratorIds, senderId, createAt) match {
        case Left(exception) =>
          if (replyTo)
            sender() ! LeaveAdministratorIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
        case Right(newThread) =>
          if (replyTo)
            sender() ! LeaveAdministratorIdsSucceeded(ULID(), cmdId, threadId, createAt)
          context.become(onCreated(newThread))
      }
  }

  private def queryAdministratorIds(thread: Thread): Receive = {
    case GetAdministratorIds(cmdId, threadId, senderId, createAt) if threadId == id =>
      thread.getAdministratorIds(senderId) match {
        case Left(exception) =>
          sender() ! GetAdministratorIdsFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
        case Right(administratorIds) =>
          sender() ! GetAdministratorIdsSucceeded(ULID(), cmdId, threadId, administratorIds, createAt)
      }
  }

  private def commandAddMessages(thread: Thread): Receive = {
    case AddMessages(requestId, threadId, messages, createAt, replyTo) if threadId == id =>
      thread.addMessages(messages, createAt) match {
        case Left(exception) =>
          if (replyTo)
            sender() ! AddMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
        case Right(newThread) =>
          if (replyTo)
            sender() ! AddMessagesSucceeded(ULID(), requestId, threadId, messages.toMessageIds, createAt)
          context.become(onCreated(newThread))
      }
  }

  private def commandRemoveMessages(thread: Thread): Receive = {
    case RemoveMessages(requestId, threadId, senderId, messageIds, createAt, replyTo) if threadId == id =>
      thread.removeMessages(messageIds, senderId, createAt) match {
        case Left(exception) =>
          if (replyTo)
            sender() ! RemoveMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
        case Right((newThread, messageIds)) =>
          if (replyTo)
            sender() ! RemoveMessagesSucceeded(ULID(), requestId, threadId, messageIds, createAt)
          context.become(onCreated(newThread))
      }
  }

  private def queryMessages(thread: Thread): Receive = {
    case GetMessages(cmdId, threadId, senderId, createAt) if threadId == id =>
      thread.getMessages(senderId) match {
        case Left(exception) =>
          sender() ! GetMessagesFailed(ULID(), cmdId, threadId, exception.getMessage, createAt)
        case Right(messages) =>
          sender() ! GetMessagesSucceeded(ULID(), cmdId, threadId, messages, createAt)
      }
  }

  private def destroy(thread: Thread): Receive = {
    case DestroyThread(requestId, threadId, senderId, createAt, replyTo) if threadId == id =>
      thread.destroy(senderId, createAt) match {
        case Left(exception) =>
          if (replyTo)
            sender() ! DestroyThreadFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
        case Right(_) =>
          if (replyTo)
            sender() ! DestroyThreadSucceeded(ULID(), requestId, threadId, createAt)
          context.become(onDestroyed)
      }
  }

  private def create: Receive = {
    case ExistsThread(requestId, threadId, _, createAt) if threadId == id =>
      sender() ! ExistsThreadSucceeded(ULID(), requestId, threadId, exists = true, createAt)
    case CreateThread(requestId, threadId, _, _, _, _, _, _, createAt, replyTo) if threadId == id =>
      if (replyTo)
        sender() ! CreateThreadFailed(ULID(), requestId, threadId, "Already created", createAt)
  }

  private def onCreated(thread: Thread): Receive =
    create
      .orElse(destroy(thread))
      .orElse(commandJoinAdministratorIds(thread))
      .orElse(commandLeaveAdministratorIds(thread))
      .orElse(queryAdministratorIds(thread))
      .orElse(commandJoinMemberIds(thread))
      .orElse(commandLeaveMemberIds(thread))
      .orElse(queryMemberIds(thread))
      .orElse(commandAddMessages(thread))
      .orElse(commandRemoveMessages(thread))
      .orElse(queryMessages(thread))

  override def preStart(): Unit = {
    subscribers.foreach(_ ! Started(ULID(), id, Instant.now))
  }

  override def postStop(): Unit = {
    subscribers.foreach(_ ! Stopped(ULID(), id, Instant.now))
  }

  override def receive: Receive = {
    case ExistsThread(requestId, threadId, _, createAt) if threadId == id =>
      sender() ! ExistsThreadSucceeded(ULID(), requestId, threadId, exists = false, createAt)
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
        reply
        ) if threadId == id =>
      if (reply)
        sender() ! CreateThreadSucceeded(ULID(), requestId, threadId, createAt)
      context.become(
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
      )
    case DestroyThread(requestId, threadId, _, createAt, replyTo) if threadId == id =>
      if (replyTo)
        sender() ! DestroyThreadFailed(ULID(), requestId, threadId, "Not created yet", createAt)
  }
}
