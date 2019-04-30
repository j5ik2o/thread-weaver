package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.CommandRequest

import scala.concurrent.duration.FiniteDuration

object ShardedThreadAggregatesProxy {

  def behavior(clusterSharding: ClusterSharding, receiveTimeout: FiniteDuration): Behavior[CommandRequest] =
    Behaviors.setup[CommandRequest] { _ =>
      val actorRef = ShardedThreadAggregates.initEntityActor(clusterSharding, receiveTimeout)
      Behaviors.receiveMessagePartial[CommandRequest] {
        case msg =>
          actorRef ! typed.ShardingEnvelope(msg.threadId.value.asString, msg)
          Behaviors.same
      }
    }

}
