package com.github.j5ik2o.threadWeaver.adaptor.http.controller

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{ Sink, Source }
import com.github.j5ik2o.threadWeaver.adaptor.http.directives.{ MetricsDirectives, ThreadValidateDirectives }
import com.github.j5ik2o.threadWeaver.adaptor.http.json._
import com.github.j5ik2o.threadWeaver.adaptor.http.presenter._
import com.github.j5ik2o.threadWeaver.adaptor.readModel.ThreadDas
import com.github.j5ik2o.threadWeaver.adaptor.http.rejections.{ NotFoundRejection, RejectionHandlers }
import com.github.j5ik2o.threadWeaver.useCase._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import kamon.context.Context
import wvlet.airframe._

trait ThreadControllerImpl extends ThreadController with ThreadValidateDirectives with MetricsDirectives {
  import com.github.j5ik2o.threadWeaver.adaptor.http.directives.ThreadValidateDirectives._

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

  private val threadDas = bind[ThreadDas]

  override def toRoutes(implicit context: Context): Route = handleRejections(RejectionHandlers.default) {
    pathPrefix("v1") {
      createThread ~ addAdministratorIds ~ addMemberIds ~ addMessages ~ getThreads ~ getAdministratorIds ~ getMemberIds ~ getMessages
    }
  }

  override private[controller] def createThread(implicit context: Context): Route =
    traceName(context)("create-thread") {
      path("threads" / "new") {
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
      path("threads" / Segment / "administrator-ids" / "add") { threadIdString =>
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
      path("threads" / Segment / "member-ids" / "add") { threadIdString =>
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
      path("threads" / Segment / "messages" / "add") { threadIdString =>
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

  override private[controller] def removeMessages(implicit context: Context): Route =
    traceName(context)("remove-messages") {
      path("threads" / Segment / "messages" / "remove") { threadIdString =>
        post {
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              entity(as[RemoveMessagesRequestJson]) { json =>
                validateRequestJson((threadId, json)).apply { commandRequest =>
                  val responseFuture = Source
                    .single(
                      commandRequest
                    ).via(
                      removeMessagesUseCase.execute
                    ).via(removeMessagesPresenter.response).runWith(Sink.head)
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

  override private[controller] def getThread(implicit context: Context) = traceName(context)("get-thread") {
    path("threads" / Segment) { threadIdString =>
      get {
        extractExecutionContext { implicit ec =>
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              onSuccess(
                threadDas
                  .getThreadByIdSource(threadId)
                  .map { threadRecord =>
                    ThreadJson(
                      threadRecord.id,
                      threadRecord.creatorId,
                      threadRecord.parentId,
                      threadRecord.title,
                      threadRecord.remarks,
                      threadRecord.createdAt.toEpochMilli,
                      threadRecord.updatedAt.toEpochMilli
                    )
                  }.runWith(Sink.headOption[ThreadJson]).map(identity)
              ) {
                case None =>
                  reject(new NotFoundRejection("thread is not found", None))
                case Some(response) =>
                  complete(GetThreadResponseJson(response))
              }

            }
          }
        }
      }
    }
  }

  override private[controller] def getThreads(implicit context: Context): Route = traceName(context)("get-threads") {
    path("threads") {
      get {
        extractExecutionContext { implicit ec =>
          extractMaterializer { implicit mat =>
            parameters(('account_id.as[String], 'offset.as[Long].?, 'limit.as[Long].?)) {
              case (accountIdString, offset, limit) =>
                validateAccountId(accountIdString) { accountId =>
                  onSuccess(
                    threadDas
                      .getThreadsByAccountIdSource(accountId, offset, limit)
                      .map { threadRecord =>
                        ThreadJson(
                          threadRecord.id,
                          threadRecord.creatorId,
                          threadRecord.parentId,
                          threadRecord.title,
                          threadRecord.remarks,
                          threadRecord.createdAt.toEpochMilli,
                          threadRecord.updatedAt.toEpochMilli
                        )
                      }.runWith(Sink.seq[ThreadJson]).map(_.toSeq)
                  ) { response =>
                    complete(GetThreadsResponseJson(response))
                  }
                }
            }
          }
        }
      }
    }
  }

  override private[controller] def getAdministratorIds(implicit context: Context): Route =
    traceName(context)("get-administrator-ids") {
      path("threads" / Segment / "administrator-ids") { threadIdString =>
        get {
          extractExecutionContext { implicit ec =>
            extractMaterializer { implicit mat =>
              validateThreadId(threadIdString) { threadId =>
                parameters(('offset.as[Long].?, 'limit.as[Long].?)) {
                  case (offset, limit) =>
                    onSuccess(
                      threadDas
                        .getAdministratorsByThreadIdSource(threadId, offset, limit)
                        .map { record =>
                          record.accountId
                        }.runWith(Sink.seq[String]).map(_.toSeq)
                    ) { response =>
                      complete(GetThreadAdministratorIdsResponseJson(response))
                    }
                }
              }
            }
          }
        }
      }
    }

  override private[controller] def getMemberIds(implicit context: Context) =
    traceName(context)("get-member-ids") {
      path("threads" / Segment / "member-ids") { threadIdString =>
        get {
          extractExecutionContext { implicit ec =>
            extractMaterializer { implicit mat =>
              validateThreadId(threadIdString) { threadId =>
                parameters(('offset.as[Long].?, 'limit.as[Long].?)) {
                  case (offset, limit) =>
                    onSuccess(
                      threadDas
                        .getMembersByThreadIdSource(threadId, offset, limit)
                        .map { record =>
                          record.accountId
                        }.runWith(Sink.seq[String]).map(_.toSeq)
                    ) { response =>
                      complete(GetThreadMemberIdsResponseJson(response))
                    }
                }
              }
            }
          }
        }
      }
    }

  override private[controller] def getMessages(implicit context: Context): Route = traceName(context)("get-messages") {
    path("threads" / Segment / "messages") { threadIdString =>
      get {
        extractExecutionContext { implicit ec =>
          extractMaterializer { implicit mat =>
            validateThreadId(threadIdString) { threadId =>
              parameters(('offset.as[Long].?, 'limit.as[Long].?)) {
                case (offset, limit) =>
                  onSuccess(
                    threadDas
                      .getMessagesByThreadIdSource(threadId, offset, limit)
                      .map { messageRecord =>
                        ThreadMessageJson(
                          messageRecord.id,
                          messageRecord.threadId,
                          messageRecord.senderId,
                          messageRecord.`type`,
                          messageRecord.body,
                          messageRecord.createdAt.toEpochMilli,
                          messageRecord.updatedAt.toEpochMilli
                        )
                      }.runWith(Sink.seq[ThreadMessageJson]).map(_.toSeq)
                  ) { response =>
                    complete(GetThreadMessagesResponseJson(response))
                  }
              }
            }
          }
        }
      }
    }
  }
}
