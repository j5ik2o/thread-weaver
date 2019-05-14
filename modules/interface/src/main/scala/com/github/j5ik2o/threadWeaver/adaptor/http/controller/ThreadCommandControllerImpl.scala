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

  private val destroyThreadUseCase   = bind[DestroyThreadUseCase]
  private val destroyThreadPresenter = bind[DestroyThreadPresenter]

  private val joinAdministratorIdsUseCase   = bind[JoinAdministratorIdsUseCase]
  private val joinAdministratorIdsPresenter = bind[JoinAdministratorIdsPresenter]

  private val leaveAdministratorIdsUseCase   = bind[LeaveAdministratorIdsUseCase]
  private val leaveAdministratorIdsPresenter = bind[LeaveAdministratorIdsPresenter]

  private val joinMemberIdsUseCase   = bind[JoinMemberIdsUseCase]
  private val joinMemberIdsPresenter = bind[JoinMemberIdsPresenter]

  private val leaveMemberIdsUseCase   = bind[LeaveMemberIdsUseCase]
  private val leaveMemberIdsPresenter = bind[LeaveMemberIdsPresenter]

  private val addMessagesUseCase   = bind[AddMessagesUseCase]
  private val addMessagesPresenter = bind[AddMessagesPresenter]

  private val removeMessagesUseCase   = bind[RemoveMessagesUseCase]
  private val removeMessagesPresenter = bind[RemoveMessagesPresenter]

  override def toRoutes(implicit context: Context): Route = handleRejections(RejectionHandlers.default) {
    pathPrefix("v1") {
      createThread ~ destroyThread ~ joinAdministratorIds ~ joinMemberIds ~ addMessages ~ removeMessages
    }
  }

  override private[controller] def createThread(implicit context: Context): Route =
    traceName(context)("create-thread") {
      path("threads" / "create") {
        post {
          extractMaterializer { implicit mat =>
            entity(as[CreateThreadRequestJson]) { json =>
              validateJsonRequest(json).apply { commandRequest =>
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

  override private[controller] def destroyThread(implicit context: Context): Route =
    traceName(context)("destroy-thread") {
      path("threads" / Segment / "destroy") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[DestroyThreadRequestJson]) { json =>
                validateJsonRequest((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(commandRequest)
                    .via(destroyThreadUseCase.execute)
                    .via(destroyThreadPresenter.response)
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

  override private[controller] def joinAdministratorIds(implicit context: Context): Route =
    traceName(context)("join-administrator-ids") {
      path("threads" / Segment / "administrator-ids" / "join") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[JoinAdministratorIdsRequestJson]) { json =>
                validateJsonRequest((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(commandRequest)
                    .via(joinAdministratorIdsUseCase.execute)
                    .via(joinAdministratorIdsPresenter.response).runWith(Sink.head)
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

  override private[controller] def leaveAdministratorIds(implicit context: Context): Route =
    traceName(context)("leave-administrator-ids") {
      path("threads" / Segment / "administrator-ids" / "leave") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[LeaveAdministratorIdsRequestJson]) { json =>
                validateJsonRequest((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(commandRequest)
                    .via(leaveAdministratorIdsUseCase.execute)
                    .via(leaveAdministratorIdsPresenter.response)
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

  override private[controller] def joinMemberIds(implicit context: Context): Route =
    traceName(context)("join-member-ids") {
      path("threads" / Segment / "member-ids" / "join") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[JoinMemberIdsRequestJson]) { json =>
                validateJsonRequest((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(commandRequest)
                    .via(joinMemberIdsUseCase.execute)
                    .via(joinMemberIdsPresenter.response)
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

  override private[controller] def leaveMemberIds(implicit context: Context): Route =
    traceName(context)("leave-member-ids") {
      path("threads" / Segment / "member-ids" / "leave") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[LeaveMemberIdsRequestJson]) { json =>
                validateJsonRequest((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(commandRequest)
                    .via(leaveMemberIdsUseCase.execute)
                    .via(leaveMemberIdsPresenter.response)
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
                validateJsonRequest((threadId, json)).apply { commandRequest =>
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
                validateJsonRequest((threadId, json)).apply { commandRequest =>
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
