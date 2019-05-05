package com.github.j5ik2o.threadWeaver.adaptor

import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.Materializer
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.CommandRequest
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.{
  ShardedThreadAggregatesProxy,
  ThreadAggregate,
  ThreadAggregates
}
import com.github.j5ik2o.threadWeaver.adaptor.controller.{ ThreadController, ThreadControllerImpl }
import com.github.j5ik2o.threadWeaver.adaptor.presenter.{ CreateThreadPresenter, CreateThreadPresenterImpl }
import com.github.j5ik2o.threadWeaver.adaptor.swagger.SwaggerDocService
import wvlet.airframe._

import scala.concurrent.duration._

object AirframeSettings {

  def designOfActorSystem(system: ActorSystem[Nothing], materializer: Materializer): Design =
    newDesign
      .bind[ActorSystem[Nothing]].toInstance(system)
      .bind[Materializer].toInstance(materializer)

  def designOfControllers: Design =
    newDesign
      .bind[ThreadController].to[ThreadControllerImpl]

  def designOfPresenters: Design =
    newDesign
      .bind[CreateThreadPresenter].to[CreateThreadPresenterImpl]

  def designOfSwagger(host: String, port: Int): Design =
    newDesign
      .bind[SwaggerDocService].toInstance(
        new SwaggerDocService(host, port, Set(classOf[ThreadController]))
      )

  def designOfShardedAggregates(clusterSharding: ClusterSharding): Design =
    newDesign
      .bind[ActorRef[CommandRequest]].toInstance {
        ActorSystem(ShardedThreadAggregatesProxy.behavior(clusterSharding, 30 seconds), name = "threads-proxy")
      }

  def designOfLocalAggregatesWithoutPersistence: Design =
    newDesign
      .bind[ActorRef[CommandRequest]].toInstance {
        ActorSystem(
          ThreadAggregates.behavior(Seq.empty, ThreadAggregate.name)(ThreadAggregate.behavior),
          name = "threads-without-persistence"
        )
      }

  def design(
      host: String,
      port: Int,
      system: ActorSystem[Nothing],
      clusterSharding: ClusterSharding,
      materializer: Materializer
  ): Design =
    com.github.j5ik2o.threadWeaver.useCase.AirframeSettings.design
      .add(designOfSwagger(host, port))
      .add(designOfActorSystem(system, materializer))
      .add(designOfPresenters)
      .add(designOfControllers)
      .add(designOfShardedAggregates(clusterSharding))

}
