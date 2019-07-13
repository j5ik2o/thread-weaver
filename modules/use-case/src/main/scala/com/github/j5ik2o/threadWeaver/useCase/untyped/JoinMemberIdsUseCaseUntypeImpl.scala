package com.github.j5ik2o.threadWeaver.useCase.untyped

import akka.NotUsed
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.JoinMemberIdsUseCase
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  JoinMemberIds => UJoinMemberIds,
  JoinMemberIdsFailed => UJoinMemberIdsFailed,
  JoinMemberIdsResponse => UJoinMemberIdsResponse,
  JoinMemberIdsSucceeded => UJoinMemberIdsSucceeded
}
import monix.execution.Scheduler

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class JoinMemberIdsUseCaseUntypeImpl(
    threadAggregates: ThreadActorRefOfCommandUntypeRef,
    parallelism: Int = 1,
    timeout: Timeout = 3 seconds
)(
    implicit system: ActorSystem
) extends JoinMemberIdsUseCase
    with UseCaseSupport {
  override def execute: Flow[UJoinMemberIds, UJoinMemberIdsResponse, NotUsed] =
    Flow[UJoinMemberIds].mapAsync(parallelism) { request =>
      implicit val to: Timeout                  = timeout
      implicit val scheduler                    = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      val future = (threadAggregates ? JoinMemberIds(
        ULID(),
        request.threadId,
        request.adderId,
        request.memberIds,
        request.createAt,
        reply = true
      )).mapTo[JoinMemberIdsResponse]
      retryBackoff(future, maxRetries, firstDelay, request.toString).runToFuture(Scheduler(ec)).map {
        case v: JoinMemberIdsSucceeded =>
          UJoinMemberIdsSucceeded(v.id, v.requestId, v.threadId, v.createAt)
        case v: JoinMemberIdsFailed =>
          UJoinMemberIdsFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
      }
    }
}
