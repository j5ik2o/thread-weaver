package com.github.j5ik2o.threadWeaver.adaptor

import akka.actor.typed.ActorSystem
import akka.stream.Materializer
import com.github.j5ik2o.threadWeaver.adaptor.controller.{ ThreadController, ThreadControllerImpl }
import com.github.j5ik2o.threadWeaver.adaptor.presenter.{ CreateThreadPresenter, CreateThreadPresenterImpl }
import com.github.j5ik2o.threadWeaver.adaptor.swagger.SwaggerDocService
import wvlet.airframe._

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

  def design(host: String, port: Int, system: ActorSystem[Nothing], materializer: Materializer): Design =
    com.github.j5ik2o.threadWeaver.useCase.AirframeSettings.design
      .add(designOfSwagger(host, port))
      .add(designOfActorSystem(system, materializer))
      .add(designOfPresenters)
      .add(designOfControllers)

}
