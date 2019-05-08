package com.github.j5ik2o.threadWeaver.useCase

import akka.actor.typed.scaladsl.AskPattern._
import akka.NotUsed
import akka.actor.Scheduler
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  AddMemberIds => UAddMemberIds,
  AddMemberIdsResponse => UAddMemberIdsResponse,
  AddMemberIdsSucceeded => UAddMemberIdsSucceeded,
  AddMemberIdsFailed => UAddMemberIdsFailed
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class AddMemberIdsUseCaseImpl(
    threadAggregates: ActorRef[CommandRequest],
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem[Nothing]
) extends AddMemberIdsUseCase {
  override def execute: Flow[UAddMemberIds, UAddMemberIdsResponse, NotUsed] =
    Flow[UAddMemberIds].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.executionContext
      threadAggregates
        .ask[AddMemberIdsResponse] { ref =>
          AddMemberIds(
            ULID(),
            request.threadId,
            request.adderId,
            request.memberIds,
            request.createAt,
            Some(ref)
          )
        }.map {
          case v: AddMemberIdsSucceeded =>
            UAddMemberIdsSucceeded(v.id, v.requestId, v.threadId, v.createAt)
          case v: AddMemberIdsFailed =>
            UAddMemberIdsFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
        }
    }
}
