package com.github.j5ik2o.threadWeaver.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.stream.{ ActorMaterializer, OverflowStrategy, QueueOfferResult }

import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success, Try }

class HttpClient(host: String, port: Int, https: Boolean = false, queueSize: Int = 10)(implicit system: ActorSystem) {

  import system.dispatcher

  implicit val mat: ActorMaterializer = ActorMaterializer()

  private val settings: ConnectionPoolSettings = ConnectionPoolSettings(system)

  private val poolClientFlow: Flow[
    (HttpRequest, Promise[HttpResponse]),
    (Try[HttpResponse], Promise[HttpResponse]),
    Http.HostConnectionPool
  ] =
    if (!https)
      Http().cachedHostConnectionPool[Promise[HttpResponse]](host, port, settings)
    else
      Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]](host, port, settings = settings)

  private val queue =
    Source
      .queue[(HttpRequest, Promise[HttpResponse])](queueSize, OverflowStrategy.dropNew)
      .via(poolClientFlow)
      .toMat(Sink.foreach({
        case (Success(resp), p) => p.success(resp)
        case (Failure(e), p)    => p.failure(e)
      }))(Keep.left)
      .run()

  def sendRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued    => responsePromise.future
      case QueueOfferResult.Dropped     => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed =>
        Future
          .failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }

}
