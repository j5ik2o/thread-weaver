package com.github.j5ik2o.threadWeaver.useCase.untyped

import akka.NotUsed
import akka.actor.{ ActorSystem, Scheduler }
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.RemoveMessagesUseCase
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  RemoveMessages => URemoveMessages,
  RemoveMessagesFailed => URemoveMessagesFailed,
  RemoveMessagesResponse => URemoveMessagesResponse,
  RemoveMessagesSucceeded => URemoveMessagesSucceeded
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class RemoveMessagesUseCaseUntypeImpl(
    threadAggregates: ThreadActorRefOfCommandUntypeRef,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem
) extends RemoveMessagesUseCase {
  override def execute: Flow[URemoveMessages, URemoveMessagesResponse, NotUsed] =
    Flow[URemoveMessages].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      (threadAggregates ? RemoveMessages(
        ULID(),
        request.threadId,
        request.removerId,
        request.messageIds,
        request.createdAt,
        reply = true
      )).mapTo[RemoveMessagesResponse].map {
        case v: RemoveMessagesSucceeded =>
          URemoveMessagesSucceeded(v.id, v.requestId, v.threadId, v.messageIds, v.createAt)
        case v: RemoveMessagesFailed =>
          URemoveMessagesFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
      }
    }
}
