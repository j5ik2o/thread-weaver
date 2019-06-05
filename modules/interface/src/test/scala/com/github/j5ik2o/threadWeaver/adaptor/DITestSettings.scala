package com.github.j5ik2o.threadWeaver.adaptor
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.Materializer
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ThreadProtocol.{
  ThreadActorRefOfCommandTypeRef,
  ThreadActorRefOfMessageTypeRef,
  ThreadReadModelUpdaterTypeRef
}
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.{
  PersistentThreadAggregate,
  ThreadAggregate,
  ThreadAggregates
}
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.ThreadActorRefOfCommandUntypeRef
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import slick.jdbc.JdbcProfile
import wvlet.airframe.{ newDesign, Design }

import scala.concurrent.duration.FiniteDuration

object DITestSettings extends DISettings {

  private[adaptor] def designOfLocalReadModelUpdater: Design =
    newDesign
      .bind[ThreadReadModelUpdaterTypeRef].toProvider[ActorSystem[Nothing], ReadJournalType, JdbcProfile, JdbcProfile#Backend#Database] {
        (actorSystem, readJournal, profile, db) =>
          actorSystem.toUntyped.spawn(
            new ThreadReadModelUpdater(readJournal, profile, db).behavior(),
            name = "local-thread-rmu"
          )
      }

  private[adaptor] def designOfLocalAggregatesWithPersistence: Design =
    newDesign
      .bind[ThreadActorRefOfCommandTypeRef].toProvider[ActorSystem[Nothing], ThreadActorRefOfMessageTypeRef] {
        (actorSystem, subscriber) =>
          actorSystem.toUntyped.spawn(
            ThreadAggregates.behavior(Seq(subscriber), ThreadAggregate.name)(PersistentThreadAggregate.behavior),
            name = "local-threads-aggregates-with-persistence"
          )
      }
      .bind[ThreadActorRefOfCommandUntypeRef].toProvider[ActorSystem[Nothing], ThreadActorRefOfMessageTypeRef] {
        (actorSystem, subscribers) =>
          actorSystem.toUntyped.actorOf(
            untyped.ThreadAggregates.props(Seq(subscribers.toUntyped), untyped.PersistentThreadAggregate.props)
          )
      }

  private[adaptor] def designOfLocalAggregatesWithoutPersistence: Design =
    newDesign
      .bind[ThreadActorRefOfCommandTypeRef].toProvider[ActorSystem[Nothing], ThreadActorRefOfMessageTypeRef] {
        (actorSystem, subscriber) =>
          actorSystem.toUntyped.spawn(
            ThreadAggregates.behavior(Seq(subscriber), ThreadAggregate.name)(ThreadAggregate.behavior),
            name = "local-threads-aggregates-without-persistence"
          )
      }
      .bind[ThreadActorRefOfCommandUntypeRef].toProvider[ActorSystem[Nothing], ThreadActorRefOfMessageTypeRef] {
        (actorSystem, subscribers) =>
          actorSystem.toUntyped.actorOf(
            untyped.ThreadAggregates.props(Seq(subscribers.toUntyped), untyped.ThreadAggregate.props)
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
      db: JdbcProfile#Backend#Database,
      aggregateAskTimeout: FiniteDuration
  ): Design =
    com.github.j5ik2o.threadWeaver.useCase.DISettings
      .design(aggregateAskTimeout)
      .add(designOfSwagger(host, port))
      .add(designOfActorSystem(system, materializer))
      .add(designOfReadJournal(readJournal))
      .add(designOfSlick(profile, db))
      .add(designOfLocalAggregatesWithPersistence)
      .add(designOfLocalReadModelUpdater)
      .add(designOfMessageRouters)
      .add(designOfRestControllers)
      .add(designOfRestPresenters)
}
