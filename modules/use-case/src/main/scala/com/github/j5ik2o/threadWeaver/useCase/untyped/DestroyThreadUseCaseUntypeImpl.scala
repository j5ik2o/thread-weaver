package com.github.j5ik2o.threadWeaver.useCase.untyped

import akka.pattern.ask
import akka.NotUsed
import akka.actor.Scheduler
import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.DestroyThreadUseCase
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  DestroyThread => UDestroyThread,
  DestroyThreadFailed => UDestroyThreadFailed,
  DestroyThreadResponse => UDestroyThreadResponse,
  DestroyThreadSucceeded => UDestroyThreadSucceeded
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class DestroyThreadUseCaseUntypeImpl(
    threadAggregates: ThreadActorRefOfCommandUntypeRef,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem
) extends DestroyThreadUseCase {
  override def execute: Flow[UDestroyThread, UDestroyThreadResponse, NotUsed] =
    Flow[UDestroyThread].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      (threadAggregates ? DestroyThread(
        ULID(),
        request.threadId,
        request.destroyerId,
        request.createAt,
        reply = true
      )).mapTo[DestroyThreadResponse].map {
        case v: DestroyThreadSucceeded =>
          UDestroyThreadSucceeded(v.id, v.requestId, v.threadId, v.createAt)
        case v: DestroyThreadFailed =>
          UDestroyThreadFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
      }
    }

}
