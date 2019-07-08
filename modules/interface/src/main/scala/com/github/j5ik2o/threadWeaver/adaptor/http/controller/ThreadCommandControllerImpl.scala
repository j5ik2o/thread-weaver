package com.github.j5ik2o.threadWeaver.adaptor.http.controller

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{ Sink, Source }
import com.github.j5ik2o.threadWeaver.adaptor.http.directives.{ MetricsDirective, ThreadValidateDirectives }
import com.github.j5ik2o.threadWeaver.adaptor.http.directives.ThreadValidateDirectives._
import com.github.j5ik2o.threadWeaver.adaptor.http.json._
import com.github.j5ik2o.threadWeaver.adaptor.http.presenter._
import com.github.j5ik2o.threadWeaver.adaptor.http.rejections.RejectionHandlers
import com.github.j5ik2o.threadWeaver.useCase._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import kamon.trace.TraceContext
import wvlet.airframe._

trait ThreadCommandControllerImpl extends ThreadCommandController with ThreadValidateDirectives with MetricsDirective {

  private val createThreadUseCase: CreateThreadUseCase     = bind[CreateThreadUseCase]
  private val createThreadPresenter: CreateThreadPresenter = bind[CreateThreadPresenter]

  private val destroyThreadUseCase: DestroyThreadUseCase     = bind[DestroyThreadUseCase]
  private val destroyThreadPresenter: DestroyThreadPresenter = bind[DestroyThreadPresenter]

  private val joinAdministratorIdsUseCase: JoinAdministratorIdsUseCase     = bind[JoinAdministratorIdsUseCase]
  private val joinAdministratorIdsPresenter: JoinAdministratorIdsPresenter = bind[JoinAdministratorIdsPresenter]

  private val leaveAdministratorIdsUseCase: LeaveAdministratorIdsUseCase     = bind[LeaveAdministratorIdsUseCase]
  private val leaveAdministratorIdsPresenter: LeaveAdministratorIdsPresenter = bind[LeaveAdministratorIdsPresenter]

  private val joinMemberIdsUseCase: JoinMemberIdsUseCase     = bind[JoinMemberIdsUseCase]
  private val joinMemberIdsPresenter: JoinMemberIdsPresenter = bind[JoinMemberIdsPresenter]

  private val leaveMemberIdsUseCase: LeaveMemberIdsUseCase = bind[LeaveMemberIdsUseCase]
  private val leaveMemberIdsPresenter                      = bind[LeaveMemberIdsPresenter]

  private val addMessagesUseCase: AddMessagesUseCase     = bind[AddMessagesUseCase]
  private val addMessagesPresenter: AddMessagesPresenter = bind[AddMessagesPresenter]

  private val removeMessagesUseCase: RemoveMessagesUseCase     = bind[RemoveMessagesUseCase]
  private val removeMessagesPresenter: RemoveMessagesPresenter = bind[RemoveMessagesPresenter]

  override def toRoutes(implicit traceContext: TraceContext): Route = handleRejections(RejectionHandlers.default) {
    pathPrefix("v1") {
      createThread ~ destroyThread ~ joinAdministratorIds ~ joinMemberIds ~ addMessages ~ removeMessages
    }
  }

  override private[controller] def createThread(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "create-thread") {
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

  override private[controller] def destroyThread(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "destroy-thread") {
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

  override private[controller] def joinAdministratorIds(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "join-administrator-ids") {
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

  override private[controller] def leaveAdministratorIds(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "leave-administrator-ids") {
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

  override private[controller] def joinMemberIds(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "join-members-ids") {
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

  override private[controller] def leaveMemberIds(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "leave-members-ids") {
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

  override private[controller] def addMessages(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "add-messages") {
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

  override private[controller] def removeMessages(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "remove-messages") {
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
