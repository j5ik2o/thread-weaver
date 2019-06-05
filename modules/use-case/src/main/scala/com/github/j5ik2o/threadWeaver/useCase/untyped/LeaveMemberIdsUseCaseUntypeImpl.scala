package com.github.j5ik2o.threadWeaver.useCase.untyped

import akka.pattern.ask
import akka.actor.ActorSystem
import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.LeaveMemberIdsUseCase
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  LeaveMemberIds => ULeaveMemberIds,
  LeaveMemberIdsFailed => ULeaveMemberIdsFailed,
  LeaveMemberIdsResponse => ULeaveMemberIdsResponse,
  LeaveMemberIdsSucceeded => ULeaveMemberIdsSucceeded
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class LeaveMemberIdsUseCaseUntypeImpl(
    threadAggregates: ThreadActorRefOfCommandUntypeRef,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem
) extends LeaveMemberIdsUseCase {
  override def execute: Flow[ULeaveMemberIds, ULeaveMemberIdsResponse, NotUsed] =
    Flow[ULeaveMemberIds].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      (threadAggregates ? LeaveMemberIds(
        ULID(),
        request.threadId,
        request.removerId,
        request.memberIds,
        request.createAt,
        reply = true
      )).mapTo[LeaveMemberIdsResponse].map {
        case v: LeaveMemberIdsSucceeded =>
          ULeaveMemberIdsSucceeded(v.id, v.requestId, v.threadId, v.createAt)
        case v: LeaveMemberIdsFailed =>
          ULeaveMemberIdsFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
      }
    }
}
