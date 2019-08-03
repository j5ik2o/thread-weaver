package com.github.j5ik2o.threadWeaver.adaptor

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ ActorSystem => UntypedActorSystem }
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.Materializer
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ShardedThreadAggregatesProxy
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ThreadProtocol.ThreadActorRefOfCommandTypeRef
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ShardedThreadAggregatesRegion
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.ThreadActorRefOfCommandUntypeRef
import com.github.j5ik2o.threadWeaver.adaptor.grpc.service.{
  ThreadCommandService,
  ThreadCommandServiceImpl,
  ThreadQueryService,
  ThreadQueryServiceImpl
}
import com.github.j5ik2o.threadWeaver.adaptor.http.controller._
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import com.github.j5ik2o.threadWeaver.adaptor.swagger.SwaggerDocService
import slick.jdbc.JdbcProfile
import wvlet.airframe._

import scala.concurrent.duration._

trait DISettings {

  private[adaptor] def designOfActorSystem(system: ActorSystem[Nothing], materializer: Materializer): Design =
    newDesign
      .bind[ActorSystem[Nothing]].toInstance(system)
      .bind[UntypedActorSystem].toInstance(system.toUntyped)
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
        new SwaggerDocService(host, port, Set(classOf[ThreadCommandController], classOf[ThreadQueryController]))
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
      .bind[http.presenter.ThreadPresenter].to[http.presenter.ThreadPresenterImpl]
      .bind[http.presenter.ThreadMessagePresenter].to[http.presenter.ThreadMessagePresenterImpl]

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

  private[adaptor] def designOfShardedAggregates(
      clusterSharding: ClusterSharding
  ): Design =
    newDesign
      .bind[ThreadActorRefOfCommandTypeRef].toProvider[UntypedActorSystem] { actorSystem =>
        actorSystem.spawn(
          ShardedThreadAggregatesProxy.behavior(clusterSharding, 30 seconds, Seq.empty),
          name = "sharded-threads-proxy-typed"
        )
      }
      .bind[ThreadActorRefOfCommandUntypeRef].toProvider[
        UntypedActorSystem,
        ReadJournalType,
        JdbcProfile,
        JdbcProfile#Backend#Database
      ] { (actorSystem, readJournal, profile, db) =>
        ShardedThreadAggregatesRegion.startClusterSharding(
          Seq.empty
        )(actorSystem)
      }

  def design(
      host: String,
      port: Int,
      system: ActorSystem[Nothing],
      clusterSharding: ClusterSharding,
      materializer: Materializer,
      profile: JdbcProfile,
      db: JdbcProfile#Backend#Database,
      aggregateAskTimeout: FiniteDuration
  ): Design =
    com.github.j5ik2o.threadWeaver.useCase.DISettings
      .designOfUntyped(aggregateAskTimeout)
      .add(designOfSwagger(host, port))
      .add(designOfActorSystem(system, materializer))
      .add(designOfSlick(profile, db))
      .add(designOfShardedAggregates(clusterSharding))
      .add(designOfRestControllers)
      .add(designOfRestPresenters)

}

object DISettings extends DISettings
