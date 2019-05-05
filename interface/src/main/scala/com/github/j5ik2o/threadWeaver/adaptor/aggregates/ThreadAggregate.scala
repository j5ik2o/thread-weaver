package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ Messages, Thread, ThreadId }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object ThreadAggregate {

  def name(id: ThreadId) = s"thread-${id.value.asString}"

  def behavior(id: ThreadId, subscribers: Seq[ActorRef[Message]]): Behavior[CommandRequest] =
    Behaviors.setup[CommandRequest] { ctx =>
      subscribers.foreach(_ ! Started(ULID(), id, Instant.now, ctx.self))

      def onDestroyed: Behavior[CommandRequest] = Behaviors.unhandled[CommandRequest]

      def onCreated(thread: Thread, lastCommandId: ULID): Behaviors.Receive[CommandRequest] =
        Behaviors.receiveMessage[CommandRequest] {
          case CreateThread(requestId, threadId, _, _, _, _, createAt, replyTo) if threadId == id =>
            replyTo.foreach(_ ! CreateThreadFailed(ULID(), requestId, threadId, "Already created", createAt))
            Behaviors.same

          case GetMessages(requestId, threadId, senderId, createAt, replyTo) if threadId == id =>
            thread.getMessages(senderId) match {
              case Left(exception) =>
                replyTo ! GetMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
              case Right(messages) =>
                replyTo ! GetMessagesSucceeded(ULID(), requestId, threadId, messages, createAt)
            }
            Behaviors.same

          case AddAdministratorIds(requestId, threadId, senderId, administratorIds, createAt, replyTo)
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

          case AddMemberIds(requestId, threadId, senderId, memberIds, createAt, replyTo)
              if threadId == id && requestId != lastCommandId =>
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

          case AddMessages(requestId, threadId, senderId, messages, createAt, replyTo)
              if threadId == id && requestId != lastCommandId =>
            thread.addMessages(messages, senderId, createAt) match {
              case Left(exception) =>
                replyTo.foreach(_ ! AddMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt))
                Behaviors.same
              case Right(newThread) =>
                replyTo.foreach(_ ! AddMessagesSucceeded(ULID(), requestId, threadId, createAt))
                onCreated(newThread, requestId)
            }

          case RemoveMessages(requestId, threadId, senderId, messageIds, createAt, replyTo)
              if threadId == id && requestId != lastCommandId =>
            thread.filterNotMessages(messageIds, senderId, createAt) match {
              case Left(exception) =>
                replyTo.foreach(_ ! RemoveMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt))
                Behaviors.same
              case Right(newThread) =>
                replyTo.foreach(_ ! RemoveMessagesSucceeded(ULID(), requestId, threadId, createAt))
                onCreated(newThread, requestId)
            }

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

      def onStarted: Behaviors.Receive[CommandRequest] = Behaviors.receiveMessage[CommandRequest] {
        case CreateThread(
            requestId,
            threadId,
            creatorId,
            parentThreadId,
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
              administratorIds,
              memberIds,
              Messages.empty,
              createAt,
              createAt
            ),
            requestId
          )

        case DestroyThread(requestId, threadId, _, createAt, replyTo) if threadId == id =>
          replyTo.foreach(_ ! DestroyThreadFailed(ULID(), requestId, threadId, "Not created yet", createAt))
          Behaviors.same

        case AddMessages(requestId, threadId, _, _, createAt, replyTo) if threadId == id =>
          replyTo.foreach(_ ! AddMessagesFailed(ULID(), requestId, threadId, "Not created yet", createAt))
          Behaviors.same

        case RemoveMessages(requestId, threadId, _, _, createAt, replyTo) if threadId == id =>
          replyTo.foreach(_ ! RemoveMessagesFailed(ULID(), requestId, threadId, "Not created yet", createAt))
          Behaviors.same
      }

      onStarted
    }

}
