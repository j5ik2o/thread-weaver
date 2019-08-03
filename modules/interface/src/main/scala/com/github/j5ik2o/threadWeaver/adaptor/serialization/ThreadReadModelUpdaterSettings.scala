package com.github.j5ik2o.threadWeaver.adaptor.serialization

import com.typesafe.config.Config

case class ThreadReadModelUpdaterSettings(category: String, numPartition: Int, shardName: String)

object ThreadReadModelUpdaterSettings {

  def fromConfig(config: Config): ThreadReadModelUpdaterSettings = new ThreadReadModelUpdaterSettings(
    category = config.getString("thread-weaver.read-model-updater.room.category"),
    numPartition = config.getInt("thread-weaver.read-model-updater.room.num-partition"),
    shardName = config.getString("thread-weaver.read-model-updater.room.shard-name")
  )

}
