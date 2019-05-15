package com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.{ CommandRequest, Message }
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId

object ThreadAggregates {

  val name = "threads"

  def behavior(
      subscribers: Seq[ActorRef[Message]],
      name: ThreadId => String
  )(behaviorF: (ThreadId, Seq[ActorRef[Message]]) => Behavior[CommandRequest]): Behavior[CommandRequest] = {
    Behaviors.setup { ctx =>
      def createAndSend(threadId: ThreadId): ActorRef[CommandRequest] = {
        ctx.child(name(threadId)) match {
          case None      => ctx.spawn(behaviorF(threadId, subscribers), name = name(threadId))
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
