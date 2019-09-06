package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import com.typesafe.config.Config

case class ThreadReadModelUpdaterSettings(category: String, numPartition: Int, shardName: String)

object ThreadReadModelUpdaterSettings {

  def fromConfig(config: Config): ThreadReadModelUpdaterSettings = new ThreadReadModelUpdaterSettings(
    category = config.getString("thread-weaver.read-model-updater.thread.category"),
    numPartition = config.getInt("thread-weaver.read-model-updater.thread.num-partition"),
    shardName = config.getString("thread-weaver.read-model-updater.thread.shard-name")
  )

}
