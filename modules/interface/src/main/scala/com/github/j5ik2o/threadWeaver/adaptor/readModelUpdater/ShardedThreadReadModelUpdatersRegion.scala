package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import akka.actor.{ ActorRef, ActorSystem }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.Duration

object ShardedThreadReadModelUpdatersRegion {

  def startClusterSharding(
      receiveTimeout: Duration,
      nrOfShards: Int,
      readJournal: ReadJournalType,
      profile: JdbcProfile,
      db: JdbcProfile#Backend#Database,
      sqlBatchSize: Long
  )(implicit system: ActorSystem): ActorRef =
    ClusterSharding(system).start(
      ShardedThreadReadModelUpdaters.shardName,
      ShardedThreadReadModelUpdaters.props(receiveTimeout, readJournal, profile, db, sqlBatchSize),
      ClusterShardingSettings(system),
      ShardedThreadReadModelUpdaters.extractEntityId,
      ShardedThreadReadModelUpdaters.extractShardId(nrOfShards)
    )

  def shardRegion(implicit system: ActorSystem): ActorRef =
    ClusterSharding(system).shardRegion(ShardedThreadReadModelUpdaters.shardName)

}
