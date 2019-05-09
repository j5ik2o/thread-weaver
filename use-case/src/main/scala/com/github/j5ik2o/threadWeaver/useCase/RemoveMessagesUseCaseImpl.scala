package com.github.j5ik2o.threadWeaver.useCase

import akka.NotUsed
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  RemoveMessages => URemoveMessages,
  RemoveMessagesFailed => URemoveMessagesFailed,
  RemoveMessagesResponse => URemoveMessagesResponse,
  RemoveMessagesSucceeded => URemoveMessagesSucceeded
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class RemoveMessagesUseCaseImpl(
    threadAggregates: ActorRef[CommandRequest],
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem[Nothing]
) extends RemoveMessagesUseCase {
  override def execute: Flow[URemoveMessages, URemoveMessagesResponse, NotUsed] =
    Flow[URemoveMessages].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.executionContext
      threadAggregates
        .ask[RemoveMessagesResponse] { ref =>
          RemoveMessages(
            ULID(),
            request.threadId,
            request.removerId,
            request.messageIds,
            request.createdAt,
            Some(ref)
          )
        }.map {
          case v: RemoveMessagesSucceeded =>
            URemoveMessagesSucceeded(v.id, v.requestId, v.threadId, v.createAt)
          case v: RemoveMessagesFailed =>
            URemoveMessagesFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
        }
    }
}
