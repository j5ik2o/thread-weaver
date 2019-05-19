package com.github.j5ik2o.threadWeaver.adaptor.grpc.service

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Sink
import akka.stream.typed.scaladsl.ActorMaterializer
import cats.data.NonEmptyList
import cats.implicits._
import com.github.j5ik2o.threadWeaver.adaptor.das.ThreadDas
import com.github.j5ik2o.threadWeaver.adaptor.error.InterfaceError
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model._
import com.github.j5ik2o.threadWeaver.adaptor.validator.ValidateUtils._
import wvlet.airframe.bind

import scala.concurrent.Future

trait ThreadQueryServiceImpl extends ThreadQueryService {
  private implicit val system = bind[ActorSystem[Nothing]]

  import system.executionContext

  implicit val mat: ActorMaterializer = ActorMaterializer()

  private val threadDas: ThreadDas = bind[ThreadDas]

  private def errorResponse[A](apply: Seq[String] => A): NonEmptyList[InterfaceError] => Future[A] = {
    f: NonEmptyList[InterfaceError] =>
      Future.successful(apply(f.map(_.message).toList))
  }

  override def getThread(in: GetThreadRequest): Future[GetThreadResponse] = {
    (
      validateThreadId(in.threadId),
      validateAccountId(in.accountId)
    ).mapN {
        case (threadId, accountId) =>
          threadDas
            .getThreadByIdSource(accountId, threadId)
            .map { threadRecord =>
              GetThreadBody(
                threadRecord.id,
                threadRecord.creatorId,
                threadRecord.parentId.isDefined,
                threadRecord.parentId.getOrElse(""),
                threadRecord.title,
                threadRecord.remarks.isDefined,
                threadRecord.remarks.getOrElse(""),
                threadRecord.createdAt.toEpochMilli,
                threadRecord.updatedAt.toEpochMilli
              )
            }.runWith(Sink.headOption[GetThreadBody]).map { body =>
              GetThreadResponse(
                isSuccessful = true,
                body,
                errorMessages = Seq.empty
              )
            }
      }.valueOr(errorResponse(GetThreadResponse(true, None, _)))
  }

  override def getThreads(in: GetThreadsRequest): Future[GetThreadsResponse] =
    validateAccountId(in.accountId)
      .map { accountId =>
        threadDas
          .getThreadsByAccountIdSource(
            accountId,
            if (in.hasOffset) Some(in.offset) else None,
            if (in.hasLimit) Some(in.limit)
            else None
          )
          .map { threadRecord =>
            GetThreadBody(
              threadRecord.id,
              threadRecord.creatorId,
              threadRecord.parentId.isDefined,
              threadRecord.parentId.getOrElse(""),
              threadRecord.title,
              threadRecord.remarks.isDefined,
              threadRecord.remarks.getOrElse(""),
              threadRecord.createdAt.toEpochMilli,
              threadRecord.updatedAt.toEpochMilli
            )
          }.runWith(Sink.seq[GetThreadBody]).map(_.toSeq).map { bodies =>
            GetThreadsResponse(
              isSuccessful = true,
              bodies,
              errorMessages = Seq.empty
            )
          }
      }.valueOr(errorResponse(GetThreadsResponse(true, Seq.empty, _)))

  override def getMessages(in: GetMessagesRequest): Future[GetMessagesResponse] =
    (validateAccountId(in.accountId), validateThreadId(in.threadId))
      .mapN {
        case (accountId, threadId) =>
          threadDas
            .getMessagesByThreadIdSource(
              accountId,
              threadId,
              if (in.hasOffset) Some(in.offset) else None,
              if (in.hasLimit) Some(in.limit)
              else None
            )
            .map { threadMessageRecord =>
              GetMessageBody(
                id = threadMessageRecord.id,
                senderId = threadMessageRecord.senderId,
                `type` = threadMessageRecord.`type`,
                body = threadMessageRecord.body,
                createdAt = threadMessageRecord.createdAt.toEpochMilli,
                updatedAt = threadMessageRecord.updatedAt.toEpochMilli
              )
            }.runWith(Sink.seq[GetMessageBody]).map(_.toSeq).map { bodies =>
              GetMessagesResponse(
                isSuccessful = true,
                bodies,
                errorMessages = Seq.empty
              )
            }
      }.valueOr(errorResponse(GetMessagesResponse(true, Seq.empty, _)))
}
