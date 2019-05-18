package com.github.j5ik2o.threadWeaver.adaptor.http.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.github.j5ik2o.threadWeaver.adaptor.http.controller.{ ThreadCommandController, ThreadQueryController }
import com.github.j5ik2o.threadWeaver.adaptor.http.directives.MetricsDirectives
import com.github.j5ik2o.threadWeaver.adaptor.swagger.SwaggerDocService

class Routes(
    val swaggerDocService: SwaggerDocService,
    threadCommandController: ThreadCommandController,
    threadQueryController: ThreadQueryController
)(
    implicit system: ActorSystem[Nothing],
    mat: Materializer
) extends MetricsDirectives {

  def root: Route =
    cors() {
      RouteLogging.default.httpLogRequestResult {
        pathEndOrSingleSlash {
          get {
            index()
          }
        } ~ path("swagger") {
          getFromResource("swagger/index.html")
        } ~ getFromResourceDirectory("swagger") ~
        swaggerDocService.routes ~ threadCommandController.toRoutes ~ threadQueryController.toRoutes
      }
    }

  def index(): Route = complete(
    HttpResponse(
      entity = HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        """<span>Wellcome to Thread Weaver API</span><br/><a href="http://localhost:18080/swagger/index.html">"""
      )
    )
  )

}
