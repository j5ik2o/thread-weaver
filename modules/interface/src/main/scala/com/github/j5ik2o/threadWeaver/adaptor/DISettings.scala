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
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ShardedThreadAggregatesProxy
import com.github.j5ik2o.threadWeaver.adaptor.grpc.service.{
  ThreadCommandService,
  ThreadCommandServiceImpl,
  ThreadQueryService,
  ThreadQueryServiceImpl
}
import com.github.j5ik2o.threadWeaver.adaptor.http.controller._
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

  private[adaptor] def designOfRestControllers: Design =
    newDesign
      .bind[ThreadCommandController].to[ThreadCommandControllerImpl]
      .bind[ThreadQueryController].to[ThreadQueryControllerImpl]

  private[adaptor] def designOfSwagger(host: String, port: Int): Design =
    newDesign
      .bind[SwaggerDocService].toInstance(
        new SwaggerDocService(host, port, Set(classOf[ThreadCommandController]))
      )

  private[adaptor] def designOfRestPresenters: Design =
    newDesign
      .bind[http.presenter.CreateThreadPresenter].to[http.presenter.CreateThreadPresenterImpl]
      .bind[http.presenter.DestroyThreadPresenter].to[http.presenter.DestroyThreadPresenterImpl]
      .bind[http.presenter.JoinAdministratorIdsPresenter].to[http.presenter.JoinAdministratorIdsPresenterImpl]
      .bind[http.presenter.LeaveAdministratorIdsPresenter].to[http.presenter.LeaveAdministratorIdsPresenterImpl]
      .bind[http.presenter.JoinMemberIdsPresenter].to[http.presenter.JoinMemberIdsPresenterImpl]
      .bind[http.presenter.LeaveMemberIdsPresenter].to[http.presenter.LeaveMemberIdsPresenterImpl]
      .bind[http.presenter.AddMessagesPresenter].to[http.presenter.AddMessagesPresenterImpl]
      .bind[http.presenter.RemoveMessagesPresenter].to[http.presenter.RemoveMessagesPresenterImpl]

  private[adaptor] def designOfGrpcServices: Design =
    newDesign
      .bind[ThreadCommandService].to[ThreadCommandServiceImpl]
      .bind[ThreadQueryService].to[ThreadQueryServiceImpl]

  private[adaptor] def designOfGrpcPresenters: Design =
    newDesign
      .bind[grpc.presenter.CreateThreadPresenter].to[grpc.presenter.CreateThreadPresenterImpl]
      .bind[grpc.presenter.DestroyThreadPresenter].to[grpc.presenter.DestroyThreadPresenterImpl]
      .bind[grpc.presenter.JoinAdministratorIdsPresenter].to[grpc.presenter.JoinAdministratorIdsPresenterImpl]
      .bind[grpc.presenter.LeaveAdministratorIdsPresenter].to[grpc.presenter.LeaveAdministratorIdsPresenterImpl]
      .bind[grpc.presenter.JoinMemberIdsPresenter].to[grpc.presenter.JoinMemberIdsPresenterImpl]
      .bind[grpc.presenter.LeaveMemberIdsPresenter].to[grpc.presenter.LeaveMemberIdsPresenterImpl]
      .bind[grpc.presenter.AddMessagesPresenter].to[grpc.presenter.AddMessagesPresenterImpl]
      .bind[grpc.presenter.RemoveMessagesPresenter].to[grpc.presenter.RemoveMessagesPresenterImpl]

  private[adaptor] def designOfReadJournal(readJournal: ReadJournalType): Design = {
    newDesign.bind[ReadJournalType].toInstance(readJournal)
  }

  private[adaptor] def designOfShardedReadModelUpdater(
      clusterSharding: ClusterSharding
  ): Design = {
    newDesign
      .bind[ThreadReadModelUpdaterRef].toProvider[ActorSystem[Nothing], ReadJournalType, JdbcProfile, JdbcProfile#Backend#Database] {
        (actorSystem, readJournal, profile, db) =>
          actorSystem.toUntyped.spawn(
            new ShardedThreadReadModelUpdaterProxy(readJournal, profile, db).behavior(clusterSharding, 30 seconds),
            name = "sharded-thread-rmu-proxy"
          )
      }
  }

  private[adaptor] def designOfShardedAggregates(
      clusterSharding: ClusterSharding
  ): Design =
    newDesign
      .bind[ThreadActorRefOfCommand].toProvider[ActorSystem[Nothing], ThreadActorRefOfMessage] {
        (actorSystem, subscriber) =>
          actorSystem.toUntyped.spawn(
            ShardedThreadAggregatesProxy.behavior(clusterSharding, 30 seconds, Seq(subscriber)),
            name = "sharded-threads-proxy"
          )
      }

  private[adaptor] def designOfMessageRouters: Design =
    newDesign
      .bind[ThreadActorRefOfMessage].toProvider[ActorSystem[Nothing], ThreadReadModelUpdaterRef] { (actorSystem, ref) =>
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
    com.github.j5ik2o.threadWeaver.useCase.DISettings.design
      .add(designOfSwagger(host, port))
      .add(designOfActorSystem(system, materializer))
      .add(designOfReadJournal(readJournal))
      .add(designOfSlick(profile, db))
      .add(designOfShardedAggregates(clusterSharding))
      .add(designOfShardedReadModelUpdater(clusterSharding))
      .add(designOfMessageRouters)
      .add(designOfRestControllers)
      .add(designOfRestPresenters)

}

object DISettings extends DISettings
