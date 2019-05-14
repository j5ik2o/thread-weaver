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
  DestroyThread => UDestroyThread,
  DestroyThreadResponse => UDestroyThreadResponse,
  DestroyThreadSucceeded => UDestroyThreadSucceeded,
  DestroyThreadFailed => UDestroyThreadFailed
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class DestroyThreadUseCaseImpl(
    threadAggregates: ThreadActorRefOfCommand,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem[Nothing]
) extends DestroyThreadUseCase {
  override def execute: Flow[UDestroyThread, UDestroyThreadResponse, NotUsed] =
    Flow[UDestroyThread].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.executionContext
      threadAggregates
        .ask[DestroyThreadResponse] { ref =>
          DestroyThread(
            ULID(),
            request.threadId,
            request.destroyerId,
            request.createAt,
            Some(ref)
          )
        }.map {
          case v: DestroyThreadSucceeded =>
            UDestroyThreadSucceeded(v.id, v.requestId, v.threadId, v.createAt)
          case v: DestroyThreadFailed =>
            UDestroyThreadFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
        }
    }
}
