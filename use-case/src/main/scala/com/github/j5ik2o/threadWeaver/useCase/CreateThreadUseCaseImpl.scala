package com.github.j5ik2o.threadWeaver.useCase

import akka.NotUsed
import akka.actor.Scheduler
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  CreateThread => UCreateThread,
  CreateThreadFailed => UCreateThreadFailed,
  CreateThreadResponse => UCreateThreadResponse,
  CreateThreadSucceeded => UCreateThreadSucceeded
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

private[useCase] class CreateThreadUseCaseImpl(
    threadAggregates: ThreadActorRefOfCommand,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem[Nothing]
) extends CreateThreadUseCase {

  override def execute: Flow[UCreateThread, UCreateThreadResponse, NotUsed] = {
    Flow[UCreateThread].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.executionContext
      threadAggregates
        .ask[CreateThreadResponse] { ref =>
          CreateThread(
            ULID(),
            request.threadId,
            request.creatorId,
            None,
            request.administratorIds,
            request.memberIds,
            request.createAt,
            Some(ref)
          )
        }.map {
          case v: CreateThreadSucceeded =>
            UCreateThreadSucceeded(v.id, v.requestId, v.threadId, v.createAt)
          case v: CreateThreadFailed =>
            UCreateThreadFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
        }
    }
  }

}
