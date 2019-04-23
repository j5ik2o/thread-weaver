package com.github.j5ik2o.threadWeave.adaptor.aggregates

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.github.j5ik2o.threadWeave.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeave.domain.model.threads.{ Messages, Thread, ThreadId }
import com.github.j5ik2o.threadWeave.infrastructure.ulid.ULID

object ThreadAggregate {

  def behavior(id: ThreadId): Behavior[CommandRequest] = Behaviors.setup[CommandRequest] { ctx =>
    def onDestroyed: Behavior[CommandRequest] = Behaviors.unhandled[CommandRequest]
    def onCreated(thread: Thread): Behaviors.Receive[CommandRequest] =
      Behaviors.receiveMessagePartial[CommandRequest] {
        case CreateThread(requestId, threadId, _, _, _, createAt, replyTo) if threadId == id =>
          replyTo.foreach(_ ! CreateThreadFailed(ULID(), requestId, threadId, "Already created", createAt))
          Behaviors.same
        case AddAdministratorIds(requestId, threadId, administratorIds, senderId, createAt, replyTo)
            if threadId == id =>
          thread.addAdministratorIds(administratorIds, senderId) match {
            case Left(exception) =>
              replyTo.foreach(
                _ ! AddAdministratorIdsFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
              )
              Behaviors.same
            case Right(newThread) =>
              replyTo.foreach(_ ! AddAdministratorIdsSucceeded(ULID(), requestId, threadId, createAt))
              onCreated(newThread)
          }
        case AddMemberIds(requestId, threadId, memberIds, senderId, createAt, replyTo) if threadId == id =>
          thread.addMemberIds(memberIds, senderId) match {
            case Left(exception) =>
              replyTo.foreach(
                _ ! AddMemberIdsFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
              )
              Behaviors.same
            case Right(newThread) =>
              replyTo.foreach(_ ! AddMemberIdsSucceeded(ULID(), requestId, threadId, createAt))
              onCreated(newThread)
          }
        case AddMessages(requestId, threadId, messages, createAt, replyTo) if threadId == id =>
          thread.addMessages(messages, createAt) match {
            case Left(exception) =>
              replyTo.foreach(_ ! AddMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt))
              Behaviors.same
            case Right(newThread) =>
              replyTo.foreach(_ ! AddMessagesSucceeded(ULID(), requestId, threadId, createAt))
              onCreated(newThread)
          }
        case RemoveMessages(requestId, threadId, messageIds, senderId, createAt, replyTo) if threadId == id =>
          thread.filterNotMessages(messageIds, senderId, createAt) match {
            case Left(exception) =>
              replyTo.foreach(_ ! RemoveMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt))
              Behaviors.same
            case Right(newThread) =>
              replyTo.foreach(_ ! RemoveMessagesSucceeded(ULID(), requestId, threadId, createAt))
              onCreated(newThread)
          }
        case DestroyThread(requestId, threadId, createAt, replyTo) if threadId == id =>
          replyTo.foreach(_ ! DestroyThreadSucceeded(ULID(), requestId, threadId, createAt))
          onDestroyed
      }
    Behaviors.receiveMessagePartial[CommandRequest] {
      case CreateThread(requestId, threadId, parentThreadId, administratorIds, memberIds, createAt, replyTo)
          if threadId == id =>
        replyTo.foreach(_ ! CreateThreadSucceeded(ULID(), requestId, threadId, createAt))
        onCreated(Thread(threadId, parentThreadId, administratorIds, memberIds, Messages.empty, createAt, createAt))
      case DestroyThread(requestId, threadId, createAt, replyTo) if threadId == id =>
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

}
