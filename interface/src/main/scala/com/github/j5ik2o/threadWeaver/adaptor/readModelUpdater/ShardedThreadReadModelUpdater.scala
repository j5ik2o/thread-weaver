package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol.{
  CommandRequest,
  Idle,
  Stop
}
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.FiniteDuration

class ShardedThreadReadModelUpdater(
    val readJournal: ReadJournalType,
    val profile: JdbcProfile,
    val db: JdbcProfile#Backend#Database
) {

  val TypeKey: EntityTypeKey[CommandRequest] = EntityTypeKey[CommandRequest]("threads-rmu")

  private def behavior(receiveTimeout: FiniteDuration): EntityContext => Behavior[CommandRequest] = { entityContext =>
    Behaviors.setup[CommandRequest] { ctx =>
      val childRef = ctx.spawn(
        new ThreadReadModelUpdater(readJournal, profile, db).behavior,
        name = "threads-rmu"
      )
      ctx.setReceiveTimeout(receiveTimeout, Idle)
      Behaviors.receiveMessagePartial {
        case Idle =>
          entityContext.shard ! ClusterSharding.Passivate(ctx.self)
          Behaviors.same
        case Stop =>
          Behaviors.stopped
        case msg =>
          childRef ! msg
          Behaviors.same
      }
    }
  }

  def initEntityActor(
      clusterSharding: ClusterSharding,
      receiveTimeout: FiniteDuration
  ): ActorRef[ShardingEnvelope[CommandRequest]] =
    clusterSharding.init(
      Entity(typeKey = TypeKey, createBehavior = behavior(receiveTimeout)).withStopMessage(Stop)
    )

}
