package com.github.j5ik2o.threadWeaver.useCase

import akka.actor.typed.scaladsl.AskPattern._
import akka.NotUsed
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import ThreadWeaverProtocol.{
  AddAdministratorIds => UAddAdministratorIds,
  AddAdministratorIdsResponse => UAddAdministratorIdsResponse,
  AddAdministratorIdsSucceeded => UAddAdministratorIdsSucceeded,
  AddAdministratorIdsFailed => UAddAdministratorIdsFailed
}
import akka.actor.Scheduler
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

private[useCase] class AddAdministratorIdsUseCaseImpl(
    threadAggregates: ActorRef[CommandRequest],
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
        .ask[AddAdministratorIdsResponse] { ref =>
          AddAdministratorIds(
            ULID(),
            request.threadId,
            request.adderId,
            request.administratorIds,
            request.createAt,
            Some(ref)
          )
        }.map {
          case v: AddAdministratorIdsSucceeded =>
            UAddAdministratorIdsSucceeded(v.id, v.requestId, v.threadId, v.createAt)
          case v: AddAdministratorIdsFailed =>
            UAddAdministratorIdsFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
        }
    }
}
