package com.github.j5ik2o.threadWeaver.adaptor.router

import java.time.Instant

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ThreadProtocol.{ Started, Stopped }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ThreadProtocol
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object AggregateToRMU {

  def behavior(rmuRef: ActorRef[ThreadReadModelUpdaterProtocol.CommandRequest]): Behavior[ThreadProtocol.Message] =
    Behaviors.setup[ThreadProtocol.Message] { ctx =>
      Behaviors.receiveMessagePartial[ThreadProtocol.Message] {
        case s: Started =>
          ctx.log.debug(s"RMU ! $s")
          rmuRef ! ThreadReadModelUpdaterProtocol.Start(ULID(), s.threadId, Instant.now)
          Behaviors.same
        case s: Stopped =>
          ctx.log.debug(s"RMU ! $s")
          rmuRef ! ThreadReadModelUpdaterProtocol.Stop(ULID(), s.threadId, Instant.now)
          Behaviors.same
      }

    }

}
