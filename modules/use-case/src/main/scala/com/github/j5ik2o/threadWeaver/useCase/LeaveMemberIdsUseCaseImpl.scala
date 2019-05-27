package com.github.j5ik2o.threadWeaver.useCase

import akka.NotUsed
import akka.actor.Scheduler
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  LeaveMemberIds => ULeaveMemberIds,
  LeaveMemberIdsFailed => ULeaveMemberIdsFailed,
  LeaveMemberIdsResponse => ULeaveMemberIdsResponse,
  LeaveMemberIdsSucceeded => ULeaveMemberIdsSucceeded
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class LeaveMemberIdsUseCaseImpl(
    threadAggregates: ThreadActorRefOfCommand,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem[Nothing]
) extends LeaveMemberIdsUseCase {

  override def execute: Flow[ULeaveMemberIds, ULeaveMemberIdsResponse, NotUsed] =
    Flow[ULeaveMemberIds].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.executionContext
      threadAggregates
        .ask[LeaveMemberIdsResponse] { ref =>
          LeaveMemberIds(
            ULID(),
            request.threadId,
            request.removerId,
            request.memberIds,
            request.createAt,
            Some(ref)
          )
        }.map {
          case v: LeaveMemberIdsSucceeded =>
            ULeaveMemberIdsSucceeded(v.id, v.requestId, v.threadId, v.createAt)
          case v: LeaveMemberIdsFailed =>
            ULeaveMemberIdsFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
        }
    }

}
