package com.github.j5ik2o.threadWeaver.adaptor.controller
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{ Sink, Source }
import com.github.j5ik2o.threadWeaver.adaptor.directives.ThreadValidateDirectives
import com.github.j5ik2o.threadWeaver.adaptor.json.CreateThreadRequestJson
import com.github.j5ik2o.threadWeaver.adaptor.presenter.CreateThreadPresenter
import com.github.j5ik2o.threadWeaver.adaptor.rejections.RejectionHandlers
import com.github.j5ik2o.threadWeaver.adaptor.routes.MetricsDirectives
import com.github.j5ik2o.threadWeaver.useCase.CreateThreadUseCase
import kamon.context.Context
import wvlet.airframe._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

trait ThreadControllerImpl extends ThreadController with ThreadValidateDirectives with MetricsDirectives {
  import com.github.j5ik2o.threadWeaver.adaptor.directives.ThreadValidateDirectives._

  private val createThreadUseCase  = bind[CreateThreadUseCase]
  private val createThradPresenter = bind[CreateThreadPresenter]

  override def toRoutes(implicit context: Context): Route = handleRejections(RejectionHandlers.default) {
    pathPrefix("v1") {
      createThread
    }
  }

  override private[controller] def createThread(implicit context: Context) = traceName(context)("create-thread") {
    extractMaterializer { implicit mat =>
      path("threads") {
        post {
          entity(as[CreateThreadRequestJson]) { json =>
            validateThreadRequestJson(json).apply { commandRequest =>
              val responseFuture = Source
                .single(
                  commandRequest
                ).via(
                  createThreadUseCase.execute
                ).via(createThradPresenter.response).runWith(Sink.head)
              onSuccess(responseFuture) { response =>
                complete(response)
              }
            }
          }
        }
      }
    }
  }
}
