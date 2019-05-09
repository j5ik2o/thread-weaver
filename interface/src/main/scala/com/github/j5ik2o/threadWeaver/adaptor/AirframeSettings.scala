package com.github.j5ik2o.threadWeaver.adaptor

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.Materializer
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.CommandRequest
import com.github.j5ik2o.threadWeaver.adaptor.aggregates._
import com.github.j5ik2o.threadWeaver.adaptor.controller.{ ThreadController, ThreadControllerImpl }
import com.github.j5ik2o.threadWeaver.adaptor.presenter._
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.{
  ShardedThreadReadModelUpdaterProxy,
  ThreadReadModelUpdater,
  ThreadReadModelUpdaterProtocol
}
import com.github.j5ik2o.threadWeaver.adaptor.router.AggregateToRMU
import com.github.j5ik2o.threadWeaver.adaptor.swagger.SwaggerDocService
import slick.jdbc.JdbcProfile
import wvlet.airframe._

import scala.concurrent.duration._

object AirframeSettings {

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
      .bind[ThreadController].to[ThreadControllerImpl]

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
        new SwaggerDocService(host, port, Set(classOf[ThreadController]))
      )

  private[adaptor] def designOfReadJournal(readJournal: ReadJournalType): Design = {
    newDesign.bind[ReadJournalType].toInstance(readJournal)
  }

  private[adaptor] def designOfShardedReadModelUpdater(actorSystem: ActorSystem[Nothing],
                                                       clusterSharding: ClusterSharding): Design = {
    newDesign
      .bind[ActorRef[ThreadReadModelUpdaterProtocol.CommandRequest]].toProvider[ReadJournalType,
                                                                                JdbcProfile,
                                                                                JdbcProfile#Backend#Database] {
        (readJournal, profile, db) =>
          actorSystem.toUntyped.spawn(
            new ShardedThreadReadModelUpdaterProxy(readJournal, profile, db).behavior(clusterSharding, 30 seconds),
            name = "sharded-thread-rmu-proxy"
          )
      }
  }

  private[adaptor] def designOfLocalReadModelUpdater(actorSystem: ActorSystem[Nothing]): Design =
    newDesign
      .bind[ActorRef[ThreadReadModelUpdaterProtocol.CommandRequest]].toProvider[ReadJournalType,
                                                                                JdbcProfile,
                                                                                JdbcProfile#Backend#Database] {
        (readJournal, profile, db) =>
          actorSystem.toUntyped.spawn(
            new ThreadReadModelUpdater(readJournal, profile, db).behavior(),
            name = "local-thread-rmu"
          )
      }

  private[adaptor] def designOfShardedAggregates(actorSystem: ActorSystem[Nothing],
                                                 clusterSharding: ClusterSharding): Design =
    newDesign
      .bind[ActorRef[ThreadProtocol.CommandRequest]].toProvider[ActorRef[ThreadProtocol.Message]] { subscriber =>
        actorSystem.toUntyped.spawn(
          ShardedThreadAggregatesProxy.behavior(clusterSharding, 30 seconds, Seq(subscriber)),
          name = "sharded-threads-proxy"
        )
      }

  private[adaptor] def designOfLocalAggregatesWithPersistence(actorSystem: ActorSystem[Nothing]): Design =
    newDesign
      .bind[ActorRef[CommandRequest]].toProvider[ActorRef[ThreadProtocol.Message]] { subscriber =>
        actorSystem.toUntyped.spawn(
          ThreadAggregates.behavior(Seq(subscriber), ThreadAggregate.name)(PersistentThreadAggregate.behavior),
          name = "local-threads-aggregates-with-persistence"
        )
      }

  private[adaptor] def designOfLocalAggregatesWithoutPersistence(actorSystem: ActorSystem[Nothing]): Design =
    newDesign
      .bind[ActorRef[CommandRequest]].toProvider[ActorRef[ThreadProtocol.Message]] { subscriber =>
        actorSystem.toUntyped.spawn(
          ThreadAggregates.behavior(Seq(subscriber), ThreadAggregate.name)(ThreadAggregate.behavior),
          name = "local-threads-aggregates"
        )
      }

  def designOfRouter(actorSystem: ActorSystem[Nothing]): Design =
    newDesign
      .bind[ActorRef[ThreadProtocol.Message]].toProvider[ActorRef[ThreadReadModelUpdaterProtocol.CommandRequest]] {
        ref =>
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
