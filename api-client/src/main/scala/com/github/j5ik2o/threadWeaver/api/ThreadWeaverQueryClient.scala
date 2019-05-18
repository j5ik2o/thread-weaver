package com.github.j5ik2o.threadWeaver.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.Uri.{ Path, Query }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Keep, Sink, Source }
import com.github.j5ik2o.threadWeaver.adaptor.http.json._

import scala.concurrent.{ ExecutionContext, Future }

final class ThreadWeaverQueryClient(settings: ClientSettings)(implicit system: ActorSystem) extends ThreadWeaverClient {

  import io.circe.generic.auto._

  implicit val mat: ActorMaterializer = ActorMaterializer()

  private val httpClient: HttpClient = new HttpClient(settings.queueSize)

  def getThread(accountId: String, threadId: String)(implicit ec: ExecutionContext): Future[GetThreadResponseJson] = {
    Source
      .single {
        HttpRequest(
          method = GET,
          uri = settings.uri
            .copy(path = Path / settings.version / "threads" / threadId).withQuery(Query("account_id" -> accountId))
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[GetThreadResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

  def getThreads(accountId: String)(implicit ec: ExecutionContext): Future[GetThreadsResponseJson] = {
    Source
      .single {
        HttpRequest(
          method = GET,
          uri =
            settings.uri.copy(path = Path / settings.version / "threads").withQuery(Query("account_id" -> accountId))
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[GetThreadsResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

  def getAdministratorIds(accountId: String, threadId: String)(
      implicit ec: ExecutionContext
  ): Future[GetThreadAdministratorIdsResponseJson] = {
    Source
      .single {
        HttpRequest(
          method = GET,
          uri = settings.uri
            .copy(path = Path / settings.version / "threads" / threadId / "administrator-ids").withQuery(
              Query("account_id" -> accountId)
            )
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[GetThreadAdministratorIdsResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

  def getMemberIds(accountId: String, threadId: String)(
      implicit ec: ExecutionContext
  ): Future[GetThreadMemberIdsResponseJson] = {
    Source
      .single {
        val queryBuilder = Query.newBuilder
        queryBuilder += ("account_id" -> accountId)
        val query = queryBuilder
          .result()
        HttpRequest(
          method = GET,
          uri = settings.uri
            .copy(path = Path / settings.version / "threads" / threadId / "member-ids").withQuery(
              Query("account_id" -> accountId)
            )
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[GetThreadMemberIdsResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

  def getMessages(accountId: String, threadId: String)(
      implicit ec: ExecutionContext
  ): Future[GetThreadMessagesResponseJson] = {
    Source
      .single {
        HttpRequest(
          method = GET,
          uri = settings.uri
            .copy(path = Path / settings.version / "threads" / threadId / "messages").withQuery(
              Query("account_id" -> accountId)
            )
        )
      }.mapAsync(1)(httpClient.sendRequest)
      .mapAsync(1) { response =>
        handleResponse(response)(_.to[GetThreadMessagesResponseJson])
      }.toMat(Sink.head)(Keep.right).run()
  }

}
