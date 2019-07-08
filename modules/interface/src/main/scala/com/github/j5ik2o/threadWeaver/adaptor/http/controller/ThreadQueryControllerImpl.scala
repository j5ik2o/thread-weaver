package com.github.j5ik2o.threadWeaver.adaptor.http.controller

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink
import com.github.j5ik2o.threadWeaver.adaptor.das.ThreadDas
import com.github.j5ik2o.threadWeaver.adaptor.http.directives.{ MetricsDirective, ThreadValidateDirectives }
import com.github.j5ik2o.threadWeaver.adaptor.http.json._
import com.github.j5ik2o.threadWeaver.adaptor.http.presenter.{ ThreadMessagePresenter, ThreadPresenter }
import com.github.j5ik2o.threadWeaver.adaptor.http.rejections.{ NotFoundRejection, RejectionHandlers }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import kamon.trace.TraceContext
import wvlet.airframe._

trait ThreadQueryControllerImpl extends ThreadQueryController with ThreadValidateDirectives with MetricsDirective {

  private val threadDas: ThreadDas                           = bind[ThreadDas]
  private val threadPresenter: ThreadPresenter               = bind[ThreadPresenter]
  private val threadMessagePresenter: ThreadMessagePresenter = bind[ThreadMessagePresenter]

  override def toRoutes(implicit traceContext: TraceContext): Route = handleRejections(RejectionHandlers.default) {
    pathPrefix("v1") {
      getThread ~ getThreads ~ getAdministratorIds ~ getMemberIds ~ getMessages
    }
  }

  override private[controller] def getThread(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "get-thread") {
      path("threads" / Segment) { threadIdString =>
        get {
          extractExecutionContext { implicit ec =>
            extractMaterializer { implicit mat =>
              validateThreadId(threadIdString) { threadId =>
                parameter('account_id) { accountValue =>
                  validateAccountId(accountValue) { accountId =>
                    onSuccess(
                      threadDas
                        .getThreadByIdSource(accountId, threadId)
                        .via(threadPresenter.response)
                        .runWith(Sink.headOption[ThreadJson]).map(identity)
                    ) {
                      case None =>
                        reject(NotFoundRejection("thread is not found", None))
                      case Some(response) =>
                        complete(GetThreadResponseJson(response))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

  override private[controller] def getThreads(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "get-threads") {
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
                        .via(threadPresenter.response)
                        .runWith(Sink.seq[ThreadJson]).map(_.toSeq)
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

  override private[controller] def getAdministratorIds(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "get-administrator-ids") {
      path("threads" / Segment / "administrator-ids") { threadIdString =>
        get {
          extractExecutionContext { implicit ec =>
            extractMaterializer { implicit mat =>
              validateThreadId(threadIdString) { threadId =>
                parameters(('account_id, 'offset.as[Long].?, 'limit.as[Long].?)) {
                  case (accountIdValue, offset, limit) =>
                    validateAccountId(accountIdValue) { accountId =>
                      onSuccess(
                        threadDas
                          .getAdministratorsByThreadIdSource(accountId, threadId, offset, limit)
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
    }

  override private[controller] def getMemberIds(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "get-members-ids") {
      path("threads" / Segment / "member-ids") { threadIdString =>
        get {
          extractExecutionContext { implicit ec =>
            extractMaterializer { implicit mat =>
              validateThreadId(threadIdString) { threadId =>
                parameters(('account_id, 'offset.as[Long].?, 'limit.as[Long].?)) {
                  case (accountIdValue, offset, limit) =>
                    validateAccountId(accountIdValue) { accountId =>
                      onSuccess(
                        threadDas
                          .getMembersByThreadIdSource(accountId, threadId, offset, limit)
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
    }

  override private[controller] def getMessages(implicit traceContext: TraceContext): Route =
    traceName(traceContext, "get-messages") {
      path("threads" / Segment / "messages") { threadIdString =>
        get {
          extractExecutionContext { implicit ec =>
            extractMaterializer { implicit mat =>
              validateThreadId(threadIdString) { threadId =>
                parameters(('account_id, 'offset.as[Long].?, 'limit.as[Long].?)) {
                  case (accountIdValue, offset, limit) =>
                    validateAccountId(accountIdValue) { accountId =>
                      onSuccess(
                        threadDas
                          .getMessagesByThreadIdSource(accountId, threadId, offset, limit)
                          .via(threadMessagePresenter.response)
                          .runWith(Sink.seq[ThreadMessageJson]).map(_.toSeq)
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
}
