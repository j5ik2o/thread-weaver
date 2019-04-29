package com.github.j5ik2o.threadWeave.adaptor.aggregates

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.github.j5ik2o.threadWeave.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeave.domain.model.threads.{ Messages, Thread, ThreadId }
import com.github.j5ik2o.threadWeave.infrastructure.ulid.ULID

object ThreadAggregate {

  def name(id: ThreadId) = s"thread-${id.value.asString}"

  def behavior(id: ThreadId): Behavior[CommandRequest] = Behaviors.setup[CommandRequest] { ctx =>
    def onDestroyed: Behavior[CommandRequest] = Behaviors.unhandled[CommandRequest]
    def onCreated(thread: Thread, lastCommandId: ULID): Behaviors.Receive[CommandRequest] =
      Behaviors.receiveMessage[CommandRequest] {
        case CreateThread(requestId, threadId, _, _, _, createAt, replyTo) if threadId == id =>
          replyTo.foreach(_ ! CreateThreadFailed(ULID(), requestId, threadId, "Already created", createAt))
          Behaviors.same

        case GetMessages(requestId, threadId, senderId, createAt, replyTo) if threadId == id =>
          if (thread.isMemberId(senderId))
            replyTo ! GetMessagesSucceeded(ULID(), requestId, threadId, thread.messages, createAt)
          else
            replyTo ! GetMessagesFailed(ULID(), requestId, threadId, "The operation is not allowed.", createAt)
          Behaviors.same

        case AddAdministratorIds(requestId, threadId, administratorIds, senderId, createAt, replyTo)
            if threadId == id && lastCommandId != requestId =>
          thread.addAdministratorIds(administratorIds, senderId) match {
            case Left(exception) =>
              replyTo.foreach(
                _ ! AddAdministratorIdsFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
              )
              Behaviors.same
            case Right(newThread) =>
              replyTo.foreach(_ ! AddAdministratorIdsSucceeded(ULID(), requestId, threadId, createAt))
              onCreated(newThread, requestId)
          }

        case AddMemberIds(requestId, threadId, memberIds, senderId, createAt, replyTo)
            if threadId == id && requestId != lastCommandId =>
          ctx.log.info("AddMemberIds: requestId = {}, last = {}, memberIds = {}", requestId, lastCommandId, memberIds)
          thread.addMemberIds(memberIds, senderId) match {
            case Left(exception) =>
              replyTo.foreach(
                _ ! AddMemberIdsFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
              )
              Behaviors.same
            case Right(newThread) =>
              replyTo.foreach(_ ! AddMemberIdsSucceeded(ULID(), requestId, threadId, createAt))
              onCreated(newThread, requestId)
          }

        case AddMessages(requestId, threadId, messages, createAt, replyTo)
            if threadId == id && requestId != lastCommandId =>
          ctx.log.info("AddMessages: requestId = {}, last = {}, messages = {}", requestId, lastCommandId, messages)
          thread.addMessages(messages, createAt) match {
            case Left(exception) =>
              replyTo.foreach(_ ! AddMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt))
              Behaviors.same
            case Right(newThread) =>
              replyTo.foreach(_ ! AddMessagesSucceeded(ULID(), requestId, threadId, createAt))
              onCreated(newThread, requestId)
          }

        case RemoveMessages(requestId, threadId, messageIds, senderId, createAt, replyTo)
            if threadId == id && requestId != lastCommandId =>
          thread.filterNotMessages(messageIds, senderId, createAt) match {
            case Left(exception) =>
              replyTo.foreach(_ ! RemoveMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt))
              Behaviors.same
            case Right(newThread) =>
              replyTo.foreach(_ ! RemoveMessagesSucceeded(ULID(), requestId, threadId, createAt))
              onCreated(newThread, requestId)
          }

        case DestroyThread(requestId, threadId, createAt, replyTo) if threadId == id =>
          replyTo.foreach(_ ! DestroyThreadSucceeded(ULID(), requestId, threadId, createAt))
          onDestroyed
      }
    def onStarted: Behaviors.Receive[CommandRequest] = Behaviors.receiveMessage[CommandRequest] {
      case CreateThread(requestId, threadId, parentThreadId, administratorIds, memberIds, createAt, replyTo)
          if threadId == id =>
        ctx.log.info("CreateThread: requestId = {}, threadId = {}, memberIds = {}", requestId, threadId, memberIds)
        replyTo.foreach(_ ! CreateThreadSucceeded(ULID(), requestId, threadId, createAt))
        onCreated(
          Thread(threadId, parentThreadId, administratorIds, memberIds, Messages.empty, createAt, createAt),
          requestId
        )

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
    onStarted
  }

}
