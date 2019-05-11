package com.github.j5ik2o.threadWeaver.adaptor

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.Materializer
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.{
  ThreadActorRefOfCommand,
  ThreadActorRefOfMessage,
  ThreadReadModelUpdaterRef
}
import com.github.j5ik2o.threadWeaver.adaptor.aggregates._
import com.github.j5ik2o.threadWeaver.adaptor.http.controller._
import com.github.j5ik2o.threadWeaver.adaptor.http.presenter._
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ShardedThreadReadModelUpdaterProxy
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import com.github.j5ik2o.threadWeaver.adaptor.router.AggregateToRMU
import com.github.j5ik2o.threadWeaver.adaptor.swagger.SwaggerDocService
import slick.jdbc.JdbcProfile
import wvlet.airframe._

import scala.concurrent.duration._

trait DISettings {

  private[adaptor] def designOfActorSystem(system: ActorSystem[Nothing], materializer: Materializer): Design =
    newDesign
      .bind[ActorSystem[Nothing]].toInstance(system)
      .bind[Materializer].toInstance(materializer)

  private[adaptor] def designOfSlick(profile: JdbcProfile, db: JdbcProfile#Backend#Database): Design =
    newDesign
      .bind[JdbcProfile].toInstance(profile)
      .bind[JdbcProfile#Backend#Database].toInstance(db)

  private[adaptor] def designOfControllers: Design =
    newDesign
      .bind[ThreadCommandController].to[ThreadCommandControllerImpl]
      .bind[ThreadQueryController].to[ThreadQueryControllerImpl]

  private[adaptor] def designOfPresenters: Design =
    newDesign
      .bind[CreateThreadPresenter].to[CreateThreadPresenterImpl]
      .bind[AddAdministratorIdsPresenter].to[AddAdministratorIdsPresenterImpl]
      .bind[AddMemberIdsPresenter].to[AddMemberIdsPresenterImpl]
      .bind[AddMessagesPresenter].to[AddMessagesPresenterImpl]
      .bind[RemoveMessagesPresenter].to[RemoveMessagesPresenterImpl]

  private[adaptor] def designOfSwagger(host: String, port: Int): Design =
    newDesign
      .bind[SwaggerDocService].toInstance(
        new SwaggerDocService(host, port, Set(classOf[ThreadCommandController]))
      )

  private[adaptor] def designOfReadJournal(readJournal: ReadJournalType): Design = {
    newDesign.bind[ReadJournalType].toInstance(readJournal)
  }

  private[adaptor] def designOfShardedReadModelUpdater(
      actorSystem: ActorSystem[Nothing],
      clusterSharding: ClusterSharding
  ): Design = {
    newDesign
      .bind[ThreadReadModelUpdaterRef].toProvider[ReadJournalType, JdbcProfile, JdbcProfile#Backend#Database] {
        (readJournal, profile, db) =>
          actorSystem.toUntyped.spawn(
            new ShardedThreadReadModelUpdaterProxy(readJournal, profile, db).behavior(clusterSharding, 30 seconds),
            name = "sharded-thread-rmu-proxy"
          )
      }
  }

  private[adaptor] def designOfShardedAggregates(
      actorSystem: ActorSystem[Nothing],
      clusterSharding: ClusterSharding
  ): Design =
    newDesign
      .bind[ThreadActorRefOfCommand].toProvider[ThreadActorRefOfMessage] { subscriber =>
        actorSystem.toUntyped.spawn(
          ShardedThreadAggregatesProxy.behavior(clusterSharding, 30 seconds, Seq(subscriber)),
          name = "sharded-threads-proxy"
        )
      }

  private[adaptor] def designOfRouter(actorSystem: ActorSystem[Nothing]): Design =
    newDesign
      .bind[ThreadActorRefOfMessage].toProvider[ThreadReadModelUpdaterRef] { ref =>
        actorSystem.toUntyped.spawn(
          AggregateToRMU.behavior(ref),
          name = "router"
        )
      }

  def design(
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
      .add(designOfShardedAggregates(system, clusterSharding))
      .add(designOfShardedReadModelUpdater(system, clusterSharding))
      .add(designOfRouter(system))
      .add(designOfControllers)
      .add(designOfPresenters)

}

object DISettings extends DISettings