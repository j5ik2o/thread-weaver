package com.github.j5ik2o.threadWeaver.api

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{ HttpRequest, MessageEntity }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Keep, Sink, Source }
import com.github.j5ik2o.threadWeaver.adaptor.http.json._

import scala.concurrent.{ ExecutionContext, Future }

final class ThreadWeaverCommandClient(settings: ClientSettings)(implicit system: ActorSystem)
    extends ThreadWeaverClient {
  import io.circe.generic.auto._

  implicit val mat: ActorMaterializer = ActorMaterializer()

  private val httpClient: HttpClient = new HttpClient(settings.queueSize)

  def createThread(
      createThreadRequestJson: CreateThreadRequestJson
  )(implicit ec: ExecutionContext): Future[CreateThreadResponseJson] = {
    Source
      .fromFuture(
        Marshal(createThreadRequestJson).to[MessageEntity]
      ).map { entity =>
        HttpRequest(
          method = POST,
          uri = settings.uri.copy(path = Path / settings.version / "threads" / "create"),
          entity = entity
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[CreateThreadResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

  def destroyThread(threadId: String, destroyThreadRequestJson: DestroyThreadRequestJson)(
      implicit ec: ExecutionContext
  ): Future[DestroyThreadResponseJson] = {
    Source
      .fromFuture(Marshal(destroyThreadRequestJson).to[MessageEntity])
      .map { entity =>
        HttpRequest(
          method = POST,
          uri = settings.uri.copy(path = Path / settings.version / "threads" / threadId / "destroy"),
          entity = entity
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[DestroyThreadResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

  def joinAdministratorIds(threadId: String, joinAdministratorIdsRequestJson: JoinAdministratorIdsRequestJson)(
      implicit ec: ExecutionContext
  ): Future[JoinAdministratorIdsResponseJson] = {
    Source
      .fromFuture(Marshal(joinAdministratorIdsRequestJson).to[MessageEntity])
      .map { entity =>
        HttpRequest(
          method = POST,
          uri = settings.uri.copy(path = Path / settings.version / "threads" / threadId / "administrator-ids" / "join"),
          entity = entity
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[JoinAdministratorIdsResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

  def leaveAdministratorIds(threadId: String, leaveAdministratorIdsRequestJson: LeaveAdministratorIdsRequestJson)(
      implicit ec: ExecutionContext
  ): Future[LeaveAdministratorIdsResponseJson] = {
    Source
      .fromFuture(Marshal(leaveAdministratorIdsRequestJson).to[MessageEntity])
      .map { entity =>
        HttpRequest(
          method = POST,
          uri = settings.uri.copy(path = Path / settings.version / "threads" / threadId / "administrator-ids" / "leave"),
          entity = entity
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[LeaveAdministratorIdsResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

  def joinMemberIds(threadId: String, joinMemberIdsRequestJson: JoinMemberIdsRequestJson)(
      implicit ec: ExecutionContext
  ): Future[JoinMemberIdsResponseJson] = {
    Source
      .fromFuture(Marshal(joinMemberIdsRequestJson).to[MessageEntity])
      .map { entity =>
        HttpRequest(
          method = POST,
          uri = settings.uri.copy(path = Path / settings.version / "threads" / threadId / "member-ids" / "join"),
          entity = entity
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[JoinMemberIdsResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

  def leaveMemberIds(threadId: String, leaveMemberIdsRequestJson: LeaveMemberIdsRequestJson)(
      implicit ec: ExecutionContext
  ): Future[LeaveMemberIdsResponseJson] = {
    Source
      .fromFuture(Marshal(leaveMemberIdsRequestJson).to[MessageEntity])
      .map { entity =>
        HttpRequest(
          method = POST,
          uri = settings.uri.copy(path = Path / settings.version / "threads" / threadId / "member-ids" / "leave"),
          entity = entity
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[LeaveMemberIdsResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

  def addMessages(threadId: String, addMessagesRequestJson: AddMessagesRequestJson)(
      implicit ec: ExecutionContext
  ): Future[AddMessagesResponseJson] = {
    Source
      .fromFuture(Marshal(addMessagesRequestJson).to[MessageEntity])
      .map { entity =>
        HttpRequest(
          method = POST,
          uri = settings.uri.copy(path = Path / settings.version / "threads" / threadId / "messages" / "add"),
          entity = entity
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[AddMessagesResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

  def removeMessages(threadId: String, removeMessagesRequestJson: RemoveMessagesRequestJson)(
      implicit ec: ExecutionContext
  ): Future[RemoveMessagesResponseJson] = {
    Source
      .fromFuture(Marshal(removeMessagesRequestJson).to[MessageEntity])
      .map { entity =>
        HttpRequest(
          method = POST,
          uri = settings.uri.copy(path = Path / settings.version / "threads" / threadId / "messages" / "remove"),
          entity = entity
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[RemoveMessagesResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

}
