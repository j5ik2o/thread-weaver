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
  AddAdministratorIds => UAddAdministratorIds,
  AddAdministratorIdsFailed => UAddAdministratorIdsFailed,
  AddAdministratorIdsResponse => UAddAdministratorIdsResponse,
  AddAdministratorIdsSucceeded => UAddAdministratorIdsSucceeded
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

private[useCase] class AddAdministratorIdsUseCaseImpl(
    threadAggregates: ThreadActorRefOfCommand,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem[Nothing]
) extends AddAdministratorIdsUseCase {
  override def execute: Flow[UAddAdministratorIds, UAddAdministratorIdsResponse, NotUsed] =
    Flow[UAddAdministratorIds].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.executionContext
      threadAggregates
        .ask[JoinAdministratorIdsResponse] { ref =>
          JoinAdministratorIds(
            ULID(),
            request.threadId,
            request.adderId,
            request.administratorIds,
            request.createAt,
            Some(ref)
          )
        }.map {
          case v: JoinAdministratorIdsSucceeded =>
            UAddAdministratorIdsSucceeded(v.id, v.requestId, v.threadId, v.createAt)
          case v: JoinAdministratorIdsFailed =>
            UAddAdministratorIdsFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
        }
    }
}
