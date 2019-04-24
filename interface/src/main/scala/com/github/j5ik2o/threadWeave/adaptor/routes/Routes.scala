package com.github.j5ik2o.threadWeave.adaptor.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.github.j5ik2o.threadWeave.adaptor.swagger.SwaggerDocService
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

class Routes(val swaggerDocService: SwaggerDocService)(
    implicit system: ActorSystem,
    mat: Materializer
) extends MetricsDirectives {

  def root: Route = apiMetrics { implicit kamonContext =>
    cors() {
      RouteLogging.default.httpLogRequestResult {
        pathEndOrSingleSlash {
          get {
            index()
          }
        } ~ path("swagger") {
          getFromResource("swagger/index.html")
        } ~ getFromResourceDirectory("swagger") ~
        swaggerDocService.routes /* ~ reactionController.toRoutes */
      }
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
