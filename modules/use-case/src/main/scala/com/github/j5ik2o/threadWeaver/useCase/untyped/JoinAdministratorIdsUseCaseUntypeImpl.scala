package com.github.j5ik2o.threadWeaver.useCase.untyped

import akka.NotUsed
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.{ ThreadActorRefOfCommandUntypeRef, _ }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.JoinAdministratorIdsUseCase
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  JoinAdministratorIds => UJoinAdministratorIds,
  JoinAdministratorIdsFailed => UJoinAdministratorIdsFailed,
  JoinAdministratorIdsResponse => UJoinAdministratorIdsResponse,
  JoinAdministratorIdsSucceeded => UJoinAdministratorIdsSucceeded
}
import monix.execution.Scheduler

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class JoinAdministratorIdsUseCaseUntypeImpl(
    threadAggregates: ThreadActorRefOfCommandUntypeRef,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem
) extends JoinAdministratorIdsUseCase
    with UseCaseSupport {
  override def execute: Flow[UJoinAdministratorIds, UJoinAdministratorIdsResponse, NotUsed] =
    Flow[UJoinAdministratorIds].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler                    = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      val future = (threadAggregates ? JoinAdministratorIds(
        ULID(),
        request.threadId,
        request.adderId,
        request.administratorIds,
        request.createAt,
        reply = true
      )).mapTo[JoinAdministratorIdsResponse]
      retryBackoff(future, maxRetries, firstDelay, request.toString).runToFuture(Scheduler(ec)).map {
        case v: JoinAdministratorIdsSucceeded =>
          UJoinAdministratorIdsSucceeded(v.id, v.requestId, v.threadId, v.createAt)
        case v: JoinAdministratorIdsFailed =>
          UJoinAdministratorIdsFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
      }
    }

}
