package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol.CommandRequest
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.FiniteDuration

class ShardedThreadReadModelUpdaterProxy(
    val readJournal: ReadJournalType,
    val profile: JdbcProfile,
    val db: JdbcProfile#Backend#Database
) {

  def behavior(clusterSharding: ClusterSharding, receiveTimeout: FiniteDuration): Behavior[CommandRequest] =
    Behaviors.setup[CommandRequest] { _ =>
      val actorRef =
        new ShardedThreadReadModelUpdater(readJournal, profile, db).initEntityActor(clusterSharding, receiveTimeout)
      Behaviors.receiveMessagePartial[CommandRequest] {
        case msg =>
          actorRef ! typed.ShardingEnvelope(msg.threadId.value.asString, msg)
          Behaviors.same
      }
    }

}
