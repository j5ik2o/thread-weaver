package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated }
import akka.persistence.{ PersistentActor, RecoveryCompleted }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadCommonProtocol
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId

object PersistentThreadAggregate {

  def props(id: ThreadId, subscribers: Seq[ActorRef]): Props =
    Props(new PersistentThreadAggregate(id, subscribers, ThreadAggregate.props))
}

class PersistentThreadAggregate(id: ThreadId, subscribers: Seq[ActorRef], propsF: (ThreadId, Seq[ActorRef]) => Props)
    extends PersistentActor
    with ActorLogging {

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: Throwable =>
      Stop
  }

  private val childRef =
    context.actorOf(propsF(id, subscribers), name = ThreadAggregate.name(id))

  context.watch(childRef)

  override def persistenceId: String = ThreadAggregate.name(id)

  override def receiveRecover: Receive = {
    case e: ThreadCommonProtocol.Event with ToCommandRequest =>
      childRef ! e.toCommandRequest
    case RecoveryCompleted =>
      log.debug("recovery completed")
  }

  private def sending(replyTo: ActorRef, event: ThreadCommonProtocol.Event): Receive = {
    case s: CommandSuccessResponse =>
      persist(event) { _ =>
        replyTo ! s
        unstashAll()
        context.unbecome()
      }
    case f: CommandFailureResponse =>
      replyTo ! f
      unstashAll()
      context.unbecome()
    case _ =>
      stash()
  }

  override def receiveCommand: Receive = {
    case Terminated(c) if c == childRef =>
      context.stop(self)
    case m: CommandRequest with ToEvent =>
      childRef ! m
      context.become(sending(sender(), m.toEvent))
    case m =>
      childRef forward m
  }

}
