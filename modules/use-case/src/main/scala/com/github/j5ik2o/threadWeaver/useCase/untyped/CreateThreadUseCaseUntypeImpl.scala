package com.github.j5ik2o.threadWeaver.useCase.untyped

import akka.NotUsed
import akka.actor.{ ActorSystem, Scheduler }
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.CreateThreadUseCase
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  CreateThread => UCreateThread,
  CreateThreadFailed => UCreateThreadFailed,
  CreateThreadResponse => UCreateThreadResponse,
  CreateThreadSucceeded => UCreateThreadSucceeded
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class CreateThreadUseCaseUntypeImpl(
    threadAggregates: ThreadActorRefOfCommandUntypeRef,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem
) extends CreateThreadUseCase {
  override def execute: Flow[UCreateThread, UCreateThreadResponse, NotUsed] =
    Flow[UCreateThread].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      (threadAggregates ? CreateThread(
        ULID(),
        request.threadId,
        request.creatorId,
        None,
        request.title,
        request.remarks,
        request.administratorIds,
        request.memberIds,
        request.createAt,
        reply = true
      )).mapTo[CreateThreadResponse].map {
        case v: CreateThreadSucceeded =>
          UCreateThreadSucceeded(v.id, v.requestId, v.threadId, v.createAt)
        case v: CreateThreadFailed =>
          UCreateThreadFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
      }
    }
}
