package com.github.j5ik2o.threadWeaver.adaptor.http.controller

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{ Sink, Source }
import com.github.j5ik2o.threadWeaver.adaptor.http.directives.ThreadValidateDirectives._
import com.github.j5ik2o.threadWeaver.adaptor.http.directives.{ MetricsDirectives, ThreadValidateDirectives }
import com.github.j5ik2o.threadWeaver.adaptor.http.json._
import com.github.j5ik2o.threadWeaver.adaptor.http.presenter._
import com.github.j5ik2o.threadWeaver.adaptor.http.rejections.RejectionHandlers
import com.github.j5ik2o.threadWeaver.useCase._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import kamon.context.Context
import wvlet.airframe._

trait ThreadCommandControllerImpl extends ThreadCommandController with ThreadValidateDirectives with MetricsDirectives {

  private val createThreadUseCase   = bind[CreateThreadUseCase]
  private val createThreadPresenter = bind[CreateThreadPresenter]

  private val addAdministratorIdsUseCase   = bind[AddAdministratorIdsUseCase]
  private val addAdministratorIdsPresenter = bind[AddAdministratorIdsPresenter]

  private val addMemberIdsUseCase   = bind[AddMemberIdsUseCase]
  private val addMemberIdsPresenter = bind[AddMemberIdsPresenter]

  private val addMessagesUseCase   = bind[AddMessagesUseCase]
  private val addMessagesPresenter = bind[AddMessagesPresenter]

  private val removeMessagesUseCase   = bind[RemoveMessagesUseCase]
  private val removeMessagesPresenter = bind[RemoveMessagesPresenter]

  override def toRoutes(implicit context: Context): Route = handleRejections(RejectionHandlers.default) {
    pathPrefix("v1") {
      createThread ~ joinAdministratorIds ~ joinMemberIds ~ addMessages
    }
  }

  override private[controller] def createThread(implicit context: Context): Route =
    traceName(context)("create-thread") {
      path("threads" / "create") {
        post {
          extractMaterializer { implicit mat =>
            entity(as[CreateThreadRequestJson]) { json =>
              validateRequestJson(json).apply { commandRequest =>
                val responseFuture = Source
                  .single(commandRequest)
                  .via(createThreadUseCase.execute)
                  .via(createThreadPresenter.response)
                  .runWith(Sink.head)
                onSuccess(responseFuture) { response =>
                  complete(response)
                }
              }
            }
          }
        }
      }
    }

  override private[controller] def joinAdministratorIds(implicit context: Context): Route =
    traceName(context)("add-administrator-ids") {
      path("threads" / Segment / "administrator-ids" / "join") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[JoinAdministratorIdsRequestJson]) { json =>
                validateRequestJson((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(commandRequest)
                    .via(addAdministratorIdsUseCase.execute)
                    .via(addAdministratorIdsPresenter.response).runWith(Sink.head)
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

  override private[controller] def joinMemberIds(implicit context: Context): Route =
    traceName(context)("add-member-ids") {
      path("threads" / Segment / "member-ids" / "join") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[JoinMemberIdsRequestJson]) { json =>
                validateRequestJson((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(commandRequest)
                    .via(addMemberIdsUseCase.execute)
                    .via(addMemberIdsPresenter.response)
                    .runWith(Sink.head)
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
      path("threads" / Segment / "messages" / "add") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[AddMessagesRequestJson]) { json =>
                validateRequestJson((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(commandRequest)
                    .via(addMessagesUseCase.execute)
                    .via(addMessagesPresenter.response)
                    .runWith(Sink.head)
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

  override private[controller] def removeMessages(implicit context: Context): Route =
    traceName(context)("remove-messages") {
      path("threads" / Segment / "messages" / "remove") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[RemoveMessagesRequestJson]) { json =>
                validateRequestJson((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(commandRequest)
                    .via(removeMessagesUseCase.execute)
                    .via(removeMessagesPresenter.response)
                    .runWith(Sink.head)
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
