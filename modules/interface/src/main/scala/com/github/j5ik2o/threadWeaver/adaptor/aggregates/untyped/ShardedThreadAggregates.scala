package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import akka.actor.{ ActorRef, Props, ReceiveTimeout }
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ShardedThreadAggregates.StopThread
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.CommandRequest
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId

object ShardedThreadAggregates {

  def props(
      subscribers: Seq[ActorRef],
      propsF: ThreadId => Seq[ActorRef] => Props
  ): Props =
    Props(new ShardedThreadAggregates(subscribers, propsF))

  def name(id: ThreadId): String = id.value.asString

  val shardName = "threads"

  case object StopThread

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: CommandRequest => (cmd.threadId.value.asString, cmd)
  }

  def extractShardId(nrOfShards: Int): ShardRegion.ExtractShardId = {
    case cmd: CommandRequest =>
      (Math.abs(cmd.threadId.value.##) % nrOfShards).toString
  }

}

class ShardedThreadAggregates(
    subscribers: Seq[ActorRef],
    propsF: ThreadId => Seq[ActorRef] => Props
) extends ThreadAggregates(subscribers, propsF) {
  context.setReceiveTimeout(Settings(context.system).passivateTimeout)

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      log.debug("ReceiveTimeout")
      context.parent ! Passivate(stopMessage = StopThread)
    case StopThread =>
      log.debug("StopWallet")
      context.stop(self)
    case msg =>
      super.unhandled(msg)
  }
}
