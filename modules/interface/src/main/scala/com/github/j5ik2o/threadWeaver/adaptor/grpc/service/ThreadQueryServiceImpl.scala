package com.github.j5ik2o.threadWeaver.adaptor.grpc.service

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Sink
import akka.stream.typed.scaladsl.ActorMaterializer
import cats.data.NonEmptyList
import cats.implicits._
import com.github.j5ik2o.threadWeaver.adaptor.das.ThreadDas
import com.github.j5ik2o.threadWeaver.adaptor.error.InterfaceError
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.{ GetThreadBody, GetThreadRequest, GetThreadResponse }
import com.github.j5ik2o.threadWeaver.adaptor.validator.ValidateUtils
import wvlet.airframe.bind

import scala.concurrent.Future

trait ThreadQueryServiceImpl extends ThreadQueryService {
  private implicit val system = bind[ActorSystem[Nothing]]

  import system.executionContext

  implicit val mat = ActorMaterializer()

  private val threadDas = bind[ThreadDas]

  private val errorResponse = { f: NonEmptyList[InterfaceError] =>
    Future.successful(GetThreadResponse(isSuccessful = false, None, f.map(_.message).toList))
  }

  override def getThread(in: GetThreadRequest): Future[GetThreadResponse] = {
    (
      ValidateUtils.validateThreadId(in.threadId),
      ValidateUtils.validateAccountId(in.senderId)
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
                Seq.empty
              )
            }
      }.valueOr(errorResponse)
  }

}
