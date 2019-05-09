package com.github.j5ik2o.threadWeaver.adaptor.controller

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{ Sink, Source }
import com.github.j5ik2o.threadWeaver.adaptor.directives.{ MetricsDirectives, ThreadValidateDirectives }
import com.github.j5ik2o.threadWeaver.adaptor.json.{
  AddAdministratorIdsRequestJson,
  AddMemberIdsRequestJson,
  AddMessagesRequestJson,
  CreateThreadRequestJson
}
import com.github.j5ik2o.threadWeaver.adaptor.presenter.{
  AddAdministratorIdsPresenter,
  AddMemberIdsPresenter,
  AddMessagesPresenter,
  CreateThreadPresenter
}
import com.github.j5ik2o.threadWeaver.adaptor.rejections.RejectionHandlers
import com.github.j5ik2o.threadWeaver.useCase.{
  AddAdministratorIdsUseCase,
  AddMemberIdsUseCase,
  AddMessagesUseCase,
  CreateThreadUseCase
}
import kamon.context.Context
import wvlet.airframe._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

trait ThreadControllerImpl extends ThreadController with ThreadValidateDirectives with MetricsDirectives {
  import com.github.j5ik2o.threadWeaver.adaptor.directives.ThreadValidateDirectives._

  private val createThreadUseCase   = bind[CreateThreadUseCase]
  private val createThreadPresenter = bind[CreateThreadPresenter]

  private val addAdministratorIdsUseCase   = bind[AddAdministratorIdsUseCase]
  private val addAdministratorIdsPresenter = bind[AddAdministratorIdsPresenter]

  private val addMemberIdsUseCase   = bind[AddMemberIdsUseCase]
  private val addMemberIdsPresenter = bind[AddMemberIdsPresenter]

  private val addMessagesUseCase   = bind[AddMessagesUseCase]
  private val addMessagesPresenter = bind[AddMessagesPresenter]

  override def toRoutes(implicit context: Context): Route = handleRejections(RejectionHandlers.default) {
    pathPrefix("v1") {
      createThread
    }
  }

  override private[controller] def createThread(implicit context: Context): Route =
    traceName(context)("create-thread") {
      path("threads") {
        post {
          extractMaterializer { implicit mat =>
            entity(as[CreateThreadRequestJson]) { json =>
              validateRequestJson(json).apply { commandRequest =>
                val responseFuture = Source
                  .single(
                    commandRequest
                  ).via(
                    createThreadUseCase.execute
                  ).via(createThreadPresenter.response).runWith(Sink.head)
                onSuccess(responseFuture) { response =>
                  complete(response)
                }
              }
            }
          }
        }
      }
    }

  override private[controller] def addAdministratorIds(implicit context: Context): Route =
    traceName(context)("add-administrator-ids") {
      path("threads" / Segment / "administrator-ids") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[AddAdministratorIdsRequestJson]) { json =>
                validateRequestJson((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(
                      commandRequest
                    ).via(
                      addAdministratorIdsUseCase.execute
                    ).via(addAdministratorIdsPresenter.response).runWith(Sink.head)
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

  override private[controller] def addMemberIds(implicit context: Context): Route =
    traceName(context)("add-member-ids") {
      path("threads" / Segment / "member-ids") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[AddMemberIdsRequestJson]) { json =>
                validateRequestJson((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(
                      commandRequest
                    ).via(
                      addMemberIdsUseCase.execute
                    ).via(addMemberIdsPresenter.response).runWith(Sink.head)
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

  override private[controller] def addMessages(implicit context: Context): Route =
    traceName(context)("add-messages") {
      path("threads" / Segment / "messages") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[AddMessagesRequestJson]) { json =>
                validateRequestJson((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(
                      commandRequest
                    ).via(
                      addMessagesUseCase.execute
                    ).via(addMessagesPresenter.response).runWith(Sink.head)
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
}
