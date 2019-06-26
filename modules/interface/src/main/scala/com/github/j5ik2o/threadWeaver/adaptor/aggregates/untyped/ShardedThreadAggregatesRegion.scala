package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import akka.actor.{ ActorRef, ActorSystem }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.PersistentThreadAggregate.ReadModelUpdaterConfig

object ShardedThreadAggregatesRegion {

  def startClusterSharding(subscribers: Seq[ActorRef], readModelUpdaterConfig: Option[ReadModelUpdaterConfig])(
      implicit system: ActorSystem
  ): ActorRef =
    ClusterSharding(system).start(
      ShardedThreadAggregates.shardName,
      ShardedThreadAggregates.props(subscribers, PersistentThreadAggregate.props, readModelUpdaterConfig),
      ClusterShardingSettings(system),
      ShardedThreadAggregates.extractEntityId,
      ShardedThreadAggregates.extractShardId
    )

  def shardRegion(implicit system: ActorSystem): ActorRef =
    ClusterSharding(system).shardRegion(ShardedThreadAggregates.shardName)
}
