package com.github.j5ik2o.threadWeaver.adaptor.serialization

import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId

import scala.util.hashing.MurmurHash3

trait JournalEventTagPartitioner {

  val numPartition: Int

  def partition(threadId: ThreadId): Int = Math.abs(MurmurHash3.stringHash(threadId.value.asString) % numPartition)

  def partitionTagName(threadId: ThreadId, category: String): String = createTagName(partition(threadId), category)

  def createTagName(id: Int, category: String): String = s"${category}-${id}"
}
