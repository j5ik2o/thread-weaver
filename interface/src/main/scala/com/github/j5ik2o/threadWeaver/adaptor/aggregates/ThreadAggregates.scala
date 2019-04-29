package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.CommandRequest
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId

object ThreadAggregates {

  val name = "threads"

  def behavior(name: ThreadId => String)(behaviorF: ThreadId => Behavior[CommandRequest]): Behavior[CommandRequest] = {
    Behaviors.setup { ctx =>
      def createAndSend(threadId: ThreadId): ActorRef[CommandRequest] = {
        ctx.child(name(threadId)) match {
          case None      => ctx.spawn(behaviorF(threadId), name = name(threadId))
          case Some(ref) => ref.asInstanceOf[ActorRef[CommandRequest]]
        }
      }
      Behaviors.receiveMessage[CommandRequest] { msg =>
        createAndSend(msg.threadId) ! msg
        Behaviors.same
      }
    }
  }
}
