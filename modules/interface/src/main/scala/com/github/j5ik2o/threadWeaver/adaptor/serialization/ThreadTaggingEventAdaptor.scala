package com.github.j5ik2o.threadWeaver.adaptor.serialization

import akka.actor.ExtendedActorSystem
import akka.persistence.journal.{ Tagged, WriteEventAdapter }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadCommonProtocol
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterSettings

class ThreadTaggingEventAdaptor(system: ExtendedActorSystem) extends WriteEventAdapter with JournalEventTagPartitioner {
  val settings = ThreadReadModelUpdaterSettings.fromConfig(system.settings.config)

  override val numPartition: Int = settings.numPartition

  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    case event: ThreadCommonProtocol.Event =>
      val tag = partitionTagName(event.threadId, settings.category)
      Tagged(event, Set(tag))
  }

}
