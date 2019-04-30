package com.github.j5ik2o.threadWeaver.useCase
import akka.NotUsed
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  CreateThread => UCreateThread,
  CreateThreadFailed => UCreateThreadFailed,
  CreateThreadResponse => UCreateThreadResponse,
  CreateThreadSucceeded => UCreateThreadSucceeded
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

private[useCase] class CreateThreadUseCaseImpl(threadAggregate: ActorRef[CommandRequest], parallelism: Int = 1)(
    implicit system: ActorSystem[Nothing]
) extends CreateThreadUseCase {

  override def execute: Flow[UCreateThread, UCreateThreadResponse, NotUsed] = {
    Flow[UCreateThread].mapAsync(parallelism) { request =>
      implicit val timeout: Timeout             = 3.seconds
      implicit val scheduler: Scheduler         = system.scheduler
      implicit val ec: ExecutionContextExecutor = system.executionContext
      threadAggregate
        .ask[CreateThreadResponse] { ref =>
          CreateThread(
            ULID(),
            request.threadId,
            None,
            request.administratorIds,
            request.memberIds,
            request.createAt,
            Some(ref)
          )
        }.map {
          case v: CreateThreadSucceeded =>
            UCreateThreadSucceeded(v.id, v.requestId, v.threadId, v.createAt)
          case v: CreateThreadFailed =>
            UCreateThreadFailed(v.id, v.requestId, v.threadId, v.message, v.createAt)
        }
    }
  }

}
