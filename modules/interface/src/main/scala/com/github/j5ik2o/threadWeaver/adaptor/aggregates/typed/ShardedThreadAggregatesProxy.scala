package com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import ThreadProtocol.{ CommandRequest, Message }

import scala.concurrent.duration.FiniteDuration

object ShardedThreadAggregatesProxy {

  def behavior(
      clusterSharding: ClusterSharding,
      receiveTimeout: FiniteDuration,
      subscribers: Seq[ActorRef[Message]]
  ): Behavior[CommandRequest] =
    Behaviors.setup[CommandRequest] { _ =>
      val actorRef = ShardedThreadAggregates.initEntityActor(clusterSharding, receiveTimeout, subscribers)
      Behaviors.receiveMessagePartial[CommandRequest] {
        case msg =>
          actorRef ! ShardingEnvelope(msg.threadId.value.asString, msg)
          Behaviors.same
      }
    }

}
