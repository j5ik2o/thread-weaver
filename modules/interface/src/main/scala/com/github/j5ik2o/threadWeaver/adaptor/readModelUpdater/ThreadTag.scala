package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import com.github.j5ik2o.threadWeaver.adaptor.serialization.ThreadReadModelUpdaterSettings
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.typesafe.config.Config

import scala.util.hashing.MurmurHash3

case class ThreadTag(value: String)

object ThreadTag {

  def fromThreadId(threadId: ThreadId)(implicit config: Config): ThreadTag = {
    val settings = ThreadReadModelUpdaterSettings.fromConfig(config)
    val num      = Math.abs(MurmurHash3.stringHash(threadId.value.asString)) % settings.numPartition
    ThreadTag(s"thread-$num")
  }

}
