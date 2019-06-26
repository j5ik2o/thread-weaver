package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import akka.actor.{ Actor, ActorContext, ActorLogging, ActorRef, Props }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.BaseCommandRequest

trait ChildActorLookup extends ActorLogging { this: Actor =>

  implicit def context: ActorContext

  type ID

  protected def childName(childId: ID): String
  protected def childProps(childId: ID): Props
  protected def toChildId(commandRequest: BaseCommandRequest): ID

  protected def forwardToActor: Actor.Receive = {
    case cmd: BaseCommandRequest =>
      context
        .child(childName(toChildId(cmd)))
        .fold(createAndForward(cmd, toChildId(cmd)))(forwardCommand(cmd))
  }

  protected def forwardCommand(cmd: BaseCommandRequest)(childRef: ActorRef): Unit =
    childRef forward cmd

  protected def createAndForward(cmd: BaseCommandRequest, childId: ID): Unit = {
    createActor(childId) forward cmd
  }

  protected def createActor(childId: ID): ActorRef =
    context.actorOf(childProps(childId), childName(childId))
}
