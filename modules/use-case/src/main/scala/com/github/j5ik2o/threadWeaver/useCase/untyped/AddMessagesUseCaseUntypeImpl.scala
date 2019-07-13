package com.github.j5ik2o.threadWeaver.useCase.untyped

import akka.NotUsed
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.{
  AddMessagesResponse,
  ThreadActorRefOfCommandUntypeRef,
  _
}
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ MessageId, Messages, TextMessage }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.AddMessagesUseCase
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  AddMessages => UAddMessages,
  AddMessagesFailed => UAddMessagesFailed,
  AddMessagesResponse => UAddMessagesResponse,
  AddMessagesSucceeded => UAddMessagesSucceeded
}
import monix.execution.Scheduler

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class AddMessagesUseCaseUntypeImpl(
    threadAggregates: ThreadActorRefOfCommandUntypeRef,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem
) extends AddMessagesUseCase
    with UseCaseSupport {
  override def execute: Flow[UAddMessages, UAddMessagesResponse, NotUsed] =
    Flow[UAddMessages].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler                    = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      val future = (threadAggregates ? AddMessages(
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
        reply = true
      )).mapTo[AddMessagesResponse]
      retryBackoff(future, maxRetries, firstDelay, request.toString).runToFuture(Scheduler(ec)).map {
        case v: AddMessagesSucceeded =>
          UAddMessagesSucceeded(v.id, v.requestId, v.threadId, v.messageIds, v.createAt)
        case v: AddMessagesFailed =>
          UAddMessagesFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
      }
    }
}
