package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import akka.actor.{ ActorRef, ActorSystem }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }

object ShardedThreadAggregatesRegion {

  def startClusterSharding(nrOfShards: Int, subscribers: Seq[ActorRef])(
      implicit system: ActorSystem
  ): ActorRef =
    ClusterSharding(system).start(
      ShardedThreadAggregates.shardName,
      ShardedThreadAggregates.props(subscribers, PersistentThreadAggregate.props),
      ClusterShardingSettings(system),
      ShardedThreadAggregates.extractEntityId,
      ShardedThreadAggregates.extractShardId(nrOfShards)
    )

  def shardRegion(implicit system: ActorSystem): ActorRef =
    ClusterSharding(system).shardRegion(ShardedThreadAggregates.shardName)
}
