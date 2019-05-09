package com.github.j5ik2o.threadWeaver.useCase

import akka.actor.typed.scaladsl.AskPattern._
import akka.NotUsed
import akka.actor.Scheduler
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  AddMessagesResponse => UAddMessagesResponse,
  AddMessages => UAddMessages,
  AddMessagesSucceeded => UAddMessagesSucceeded,
  AddMessagesFailed => UAddMessagesFailed
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

private[useCase] class AddMessagesUseCaseImpl(
    threadAggregates: ActorRef[CommandRequest],
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem[Nothing]
) extends AddMessagesUseCase {
  override def execute: Flow[UAddMessages, UAddMessagesResponse, NotUsed] = Flow[UAddMessages].mapAsync(parallelism) {
    request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.executionContext
      threadAggregates
        .ask[AddMessagesResponse] { ref =>
          AddMessages(
            ULID(),
            request.threadId,
            Messages(
              request.messages
                .map { message =>
                  TextMessage(
                    MessageId(),
                    message.replyMessageId,
                    message.toAccountIds,
                    message.body,
                    message.senderId,
                    request.createdAt,
                    request.createdAt
                  )
                }: _*
            ),
            request.createdAt,
            Some(ref)
          )
        }.map {
          case v: AddMessagesSucceeded =>
            UAddMessagesSucceeded(v.id, v.requestId, v.threadId, v.messageIds, v.createAt)
          case v: AddMessagesFailed =>
            UAddMessagesFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
        }
  }
}
