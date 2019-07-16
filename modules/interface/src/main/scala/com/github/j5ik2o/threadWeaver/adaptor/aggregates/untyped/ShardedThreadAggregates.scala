package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import akka.actor.{ ActorRef, Props, ReceiveTimeout }
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.PersistentThreadAggregate.ReadModelUpdaterConfig
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ShardedThreadAggregates.StopThread
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.CommandRequest
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId

object ShardedThreadAggregates {

  def props(
      subscribers: Seq[ActorRef],
      propsF: Option[ReadModelUpdaterConfig] => ThreadId => Seq[ActorRef] => Props,
      readModelUpdaterConfig: Option[ReadModelUpdaterConfig]
  ): Props =
    Props(new ShardedThreadAggregates(subscribers, propsF, readModelUpdaterConfig))

  def name(id: ThreadId): String = id.value.asString

  val shardName = "threads"

  case object StopThread

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: CommandRequest => (cmd.threadId.value.asString, cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: CommandRequest =>
      val mostSignificantBits  = cmd.threadId.value.mostSignificantBits  % 3
      val leastSignificantBits = cmd.threadId.value.leastSignificantBits % 3
      s"$mostSignificantBits:$leastSignificantBits"
  }

}

class ShardedThreadAggregates(
    subscribers: Seq[ActorRef],
    propsF: Option[ReadModelUpdaterConfig] => ThreadId => Seq[ActorRef] => Props,
    readModelUpdaterConfig: Option[ReadModelUpdaterConfig]
) extends ThreadAggregates(subscribers, propsF(readModelUpdaterConfig)) {
  context.setReceiveTimeout(Settings(context.system).passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      log.debug("ReceiveTimeout")
      context.parent ! Passivate(stopMessage = StopThread)
    case StopThread =>
      log.debug("StopWallet")
      context.stop(self)
  }
}
