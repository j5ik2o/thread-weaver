package com.github.j5ik2o.threadWeaver.adaptor

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.Materializer
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.{
  PersistentThreadAggregate,
  ThreadAggregate,
  ThreadAggregates
}
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.{
  ThreadActorRefOfCommand,
  ThreadActorRefOfMessage,
  ThreadReadModelUpdaterRef
}
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import slick.jdbc.JdbcProfile
import wvlet.airframe.{ newDesign, Design }

object DITestSettings extends DISettings {
  private[adaptor] def designOfLocalReadModelUpdater(actorSystem: ActorSystem[Nothing]): Design =
    newDesign
      .bind[ThreadReadModelUpdaterRef].toProvider[ReadJournalType, JdbcProfile, JdbcProfile#Backend#Database] {
        (readJournal, profile, db) =>
          actorSystem.toUntyped.spawn(
            new ThreadReadModelUpdater(readJournal, profile, db).behavior(),
            name = "local-thread-rmu"
          )
      }
  private[adaptor] def designOfLocalAggregatesWithPersistence(actorSystem: ActorSystem[Nothing]): Design =
    newDesign
      .bind[ThreadActorRefOfCommand].toProvider[ThreadActorRefOfMessage] { subscriber =>
        actorSystem.toUntyped.spawn(
          ThreadAggregates.behavior(Seq(subscriber), ThreadAggregate.name)(PersistentThreadAggregate.behavior),
          name = "local-threads-aggregates-with-persistence"
        )
      }

  private[adaptor] def designOfLocalAggregatesWithoutPersistence(actorSystem: ActorSystem[Nothing]): Design =
    newDesign
      .bind[ThreadActorRefOfCommand].toProvider[ThreadActorRefOfMessage] { subscriber =>
        actorSystem.toUntyped.spawn(
          ThreadAggregates.behavior(Seq(subscriber), ThreadAggregate.name)(ThreadAggregate.behavior),
          name = "local-threads-aggregates-without-persistence"
        )
      }

  override def design(
      host: String,
      port: Int,
      system: ActorSystem[Nothing],
      clusterSharding: ClusterSharding,
      materializer: Materializer,
      readJournal: ReadJournalType,
      profile: JdbcProfile,
      db: JdbcProfile#Backend#Database
  ): Design =
    com.github.j5ik2o.threadWeaver.useCase.AirframeSettings.design
      .add(designOfSwagger(host, port))
      .add(designOfActorSystem(system, materializer))
      .add(designOfReadJournal(readJournal))
      .add(designOfSlick(profile, db))
      .add(designOfLocalAggregatesWithPersistence(system))
      .add(designOfLocalReadModelUpdater(system))
      .add(designOfRouter(system))
      .add(designOfControllers)
      .add(designOfPresenters)
}
