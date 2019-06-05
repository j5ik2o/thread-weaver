package com.github.j5ik2o.threadWeaver.useCase.untyped

import akka.NotUsed
import akka.actor.{ ActorSystem, Scheduler }
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.LeaveAdministratorIdsUseCase
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  LeaveAdministratorIds => ULeaveAdministratorIds,
  LeaveAdministratorIdsFailed => ULeaveAdministratorIdsFailed,
  LeaveAdministratorIdsResponse => ULeaveAdministratorIdsResponse,
  LeaveAdministratorIdsSucceeded => ULeaveAdministratorIdsSucceeded
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class LeaveAdministratorIdsUseCaseUntypeImpl(
    threadAggregates: ThreadActorRefOfCommandUntypeRef,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem
) extends LeaveAdministratorIdsUseCase {
  override def execute: Flow[ULeaveAdministratorIds, ULeaveAdministratorIdsResponse, NotUsed] =
    Flow[ULeaveAdministratorIds].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      (threadAggregates ? LeaveAdministratorIds(
        ULID(),
        request.threadId,
        request.removerId,
        request.administratorIds,
        request.createAt,
        reply = true
      )).mapTo[LeaveAdministratorIdsResponse].map {
        case v: LeaveAdministratorIdsSucceeded =>
          ULeaveAdministratorIdsSucceeded(v.id, v.requestId, v.threadId, v.createAt)
        case v: LeaveAdministratorIdsFailed =>
          ULeaveAdministratorIdsFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
      }
    }
}
