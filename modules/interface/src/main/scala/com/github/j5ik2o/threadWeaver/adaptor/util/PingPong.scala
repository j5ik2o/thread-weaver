package com.github.j5ik2o.threadWeaver.adaptor.util

import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object PingPong extends App {
  trait Message
  case class Ping(reply: ActorRef[Message]) extends Message
  case object Pong                          extends Message

  def receiver: Behavior[Message] =
    Behaviors.setup[Message] { ctx =>
      Behaviors.receiveMessagePartial[Message] {
        case Ping(replyTo) =>
          ctx.log.info("ping")
          replyTo ! Pong
          Behaviors.same
      }
    }

  def main: Behavior[Message] = Behaviors.setup { ctx =>
    val receiverRef = ctx.spawn(receiver, name = "receiver")
    receiverRef ! Ping(ctx.self)
    Behaviors.receiveMessagePartial[Message] {
      case Pong =>
        ctx.log.info("pong")
        receiverRef ! Ping(ctx.self)
        Behaviors.same
    }
  }

  ActorSystem(main, "ping-pong")

}
