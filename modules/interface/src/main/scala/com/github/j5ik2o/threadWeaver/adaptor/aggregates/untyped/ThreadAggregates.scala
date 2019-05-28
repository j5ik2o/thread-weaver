package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId

object ThreadAggregates {

  val name = "threads"

  def props(subscribers: Seq[ActorRef], propsF: (ThreadId, Seq[ActorRef]) => Props): Props =
    Props(new ThreadAggregates(subscribers, propsF))

}

class ThreadAggregates(subscribers: Seq[ActorRef], propsF: (ThreadId, Seq[ActorRef]) => Props)
    extends Actor
    with ActorLogging
    with ChildActorLookup {
  override def receive: Receive = forwardToActor

  override type ID             = ThreadId
  override type CommandRequest = ThreadProtocol.CommandMessage

  override protected def childName(childId: ThreadId): String = childId.value.asString

  override protected def childProps(childId: ThreadId): Props = propsF(childId, subscribers)

  override protected def toChildId(commandRequest: ThreadProtocol.CommandMessage): ThreadId = commandRequest.threadId
}
