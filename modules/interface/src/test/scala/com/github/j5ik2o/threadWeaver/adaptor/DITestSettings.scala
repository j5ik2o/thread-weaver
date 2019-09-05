package com.github.j5ik2o.threadWeaver.adaptor

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.Materializer
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ThreadProtocol.{
  ThreadActorRefOfCommandTypeRef,
  ThreadReadModelUpdaterRef
}
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.{
  PersistentThreadAggregate,
  ThreadAggregate,
  ThreadAggregates
}
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.ThreadActorRefOfCommandUntypeRef
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import com.github.j5ik2o.threadWeaver.adaptor.routing.ThreadToRMURouter
import slick.jdbc.JdbcProfile
import wvlet.airframe.{ newDesign, Design }

import scala.concurrent.duration.FiniteDuration

object DITestSettings extends DISettings {
  private[adaptor] def designOfLocalReadModelUpdater(
      sqlBatchSize: Long
  ): Design = {
    newDesign
      .bind[ThreadReadModelUpdaterRef].toProvider[
        ActorSystem[Nothing],
        ReadJournalType,
        JdbcProfile,
        JdbcProfile#Backend#Database
      ] {
        case (actorSystem, readJournal, profile, db) =>
          actorSystem.toUntyped.actorOf(
            ThreadReadModelUpdater.props(
              readJournal,
              profile,
              db,
              sqlBatchSize,
              None
            )
          )
      }
  }
  private[adaptor] def designOfLocalAggregatesWithPersistence: Design =
    newDesign
      .bind[ThreadActorRefOfCommandTypeRef].toProvider[ActorSystem[Nothing]] { actorSystem =>
        actorSystem.toUntyped.spawn(
          ThreadAggregates.behavior(Seq.empty, ThreadAggregate.name)(PersistentThreadAggregate.behavior),
          name = "local-threads-aggregates-with-persistence"
        )
      }
      .bind[ThreadActorRefOfCommandUntypeRef].toProvider[ActorSystem[Nothing], ThreadReadModelUpdaterRef] {
        case (actorSystem, rmuRef) =>
          val router =
            actorSystem.toUntyped.actorOf(ThreadToRMURouter.props(rmuRef))
          actorSystem.toUntyped.actorOf(
            untyped.ThreadAggregates.props(
              Seq(router),
              untyped.PersistentThreadAggregate.props
            )
          )
      }

  private[adaptor] def designOfLocalAggregatesWithoutPersistence: Design =
    newDesign
      .bind[ThreadActorRefOfCommandTypeRef].toProvider[ActorSystem[Nothing]] { actorSystem =>
        actorSystem.toUntyped.spawn(
          ThreadAggregates.behavior(Seq.empty, ThreadAggregate.name)(ThreadAggregate.behavior),
          name = "local-threads-aggregates-without-persistence"
        )
      }
      .bind[ThreadActorRefOfCommandUntypeRef].toProvider[ActorSystem[Nothing]] { actorSystem =>
        actorSystem.toUntyped.actorOf(
          untyped.ThreadAggregates.props(Seq.empty, (untyped.ThreadAggregate.props _).curried)
        )
      }

  override def designOfAPI(
      host: String,
      port: Int,
      system: ActorSystem[Nothing],
      clusterSharding: ClusterSharding,
      materializer: Materializer,
      profile: JdbcProfile,
      db: JdbcProfile#Backend#Database,
      aggregateAskTimeout: FiniteDuration,
      nrOfShardOfAggregates: Int
  ): Design =
    com.github.j5ik2o.threadWeaver.useCase.DISettings
      .design(aggregateAskTimeout)
      .add(designOfSwagger(host, port))
      .add(designOfActorSystem(system, materializer))
      .add(designOfSlick(profile, db))
      .add(designOfLocalReadModelUpdater(10))
      .add(designOfLocalAggregatesWithPersistence)
      .add(designOfRestControllers)
      .add(designOfRestPresenters)
}
