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
  LeaveAdministratorIds => ULeaveAdministratorIds,
  LeaveAdministratorIdsFailed => ULeaveAdministratorIdsFailed,
  LeaveAdministratorIdsResponse => ULeaveAdministratorIdsResponse,
  LeaveAdministratorIdsSucceeded => ULeaveAdministratorIdsSucceeded
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class LeaveAdministratorIdsUseCaseImpl(
    threadAggregates: ThreadActorRefOfCommand,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem[Nothing]
) extends LeaveAdministratorIdsUseCase {

  override def execute: Flow[ULeaveAdministratorIds, ULeaveAdministratorIdsResponse, NotUsed] =
    Flow[ULeaveAdministratorIds].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.executionContext
      threadAggregates
        .ask[LeaveAdministratorIdsResponse] { ref =>
          LeaveAdministratorIds(
            ULID(),
            request.threadId,
            request.removerId,
            request.administratorIds,
            request.createAt,
            Some(ref)
          )
        }.map {
          case v: LeaveAdministratorIdsSucceeded =>
            ULeaveAdministratorIdsSucceeded(v.id, v.requestId, v.threadId, v.createAt)
          case v: LeaveAdministratorIdsFailed =>
            ULeaveAdministratorIdsFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
        }
    }

}
