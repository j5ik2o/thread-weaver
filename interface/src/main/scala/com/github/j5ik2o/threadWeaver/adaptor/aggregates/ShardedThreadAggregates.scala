package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.{ CommandRequest, Idle, Stop }

import scala.concurrent.duration.FiniteDuration

object ShardedThreadAggregates {

  val TypeKey: EntityTypeKey[CommandRequest] = EntityTypeKey[CommandRequest]("threads")

  private def behavior(receiveTimeout: FiniteDuration): EntityContext => Behavior[CommandRequest] = { entityContext =>
    Behaviors.setup[CommandRequest] { ctx =>
      val childRef = ctx.spawn(
        ThreadAggregates.behavior(_.value.asString)(PersistentThreadAggregate.behavior),
        name = ThreadAggregates.name
      )
      ctx.setReceiveTimeout(receiveTimeout, Idle)
      Behaviors.receiveMessagePartial {
        case Idle =>
          entityContext.shard ! ClusterSharding.Passivate(ctx.self)
          Behaviors.same
        case Stop =>
          Behaviors.stopped
        case msg =>
          childRef ! msg
          Behaviors.same
      }
    }
  }

  def initEntityActor(
      clusterSharding: ClusterSharding,
      receiveTimeout: FiniteDuration
  ): ActorRef[ShardingEnvelope[CommandRequest]] =
    clusterSharding.init(
      Entity(typeKey = ShardedThreadAggregates.TypeKey, createBehavior = behavior(receiveTimeout)).withStopMessage(Stop)
    )

}
