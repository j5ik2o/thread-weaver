package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import akka.actor.{ Props, ReceiveTimeout }
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ShardedThreadReadModelUpdaters.StopThread
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol.CommandRequest
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.Duration

object ShardedThreadReadModelUpdaters {

  def props(
      receiveTimeout: Duration,
      readJournal: ReadJournalType,
      profile: JdbcProfile,
      db: JdbcProfile#Backend#Database,
      sqlBatchSize: Long
  ): Props =
    Props(new ShardedThreadReadModelUpdaters(receiveTimeout, readJournal, profile, db, sqlBatchSize))

  def name(id: ThreadTag): String = id.value
  val shardName                   = "thread-read-model-updaters"

  case object StopThread

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: CommandRequest => (cmd.threadTag.value, cmd)
  }

  def extractShardId(nrOfShards: Int): ShardRegion.ExtractShardId = {
    case cmd: CommandRequest =>
      (Math.abs(cmd.threadTag.value.##) % nrOfShards).toString
  }

}

class ShardedThreadReadModelUpdaters(
    receiveTimeout: Duration,
    readJournal: ReadJournalType,
    profile: JdbcProfile,
    db: JdbcProfile#Backend#Database,
    sqlBatchSize: Long
) extends ThreadReadModelUpdaters(readJournal, profile, db, sqlBatchSize) {
  context.setReceiveTimeout(receiveTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      log.debug("ReceiveTimeout")
      context.parent ! Passivate(stopMessage = StopThread)
    case StopThread =>
      log.debug("StopThread")
      context.stop(self)
    case msg =>
      log.debug(s"unhandled message: $msg")
      super.unhandled(msg)
  }
}
