package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.{
  BackoffSettings,
  ReadJournalType
}
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

  private def behavior(receiveTimeout: FiniteDuration,
                       sqlBatchSize: Long = 10,
                       backoffSettings: Option[BackoffSettings] = None): EntityContext => Behavior[CommandRequest] = {
    entityContext =>
      Behaviors.setup[CommandRequest] { ctx =>
        val childRef = ctx.spawn(
          new ThreadReadModelUpdater(readJournal, profile, db).behavior(sqlBatchSize, backoffSettings),
          name = "threads-rmu"
        )
        Behaviors.receiveMessagePartial {
          case Idle =>
            entityContext.shard ! ClusterSharding.Passivate(ctx.self)
            Behaviors.same
          case Stop =>
            Behaviors.stopped
          case Stop(_, _, _) =>
            ctx.self ! Idle
            Behaviors.same
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
