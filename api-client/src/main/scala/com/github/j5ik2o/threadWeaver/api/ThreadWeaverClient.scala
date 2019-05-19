package com.github.j5ik2o.threadWeaver.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.github.j5ik2o.threadWeaver.adaptor.http.json.ErrorsResponseJson
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.concurrent.{ ExecutionContext, Future }

trait ThreadWeaverClient extends FailFastCirceSupport {
  import io.circe.generic.auto._
  protected def handleResponse[A](
      response: HttpResponse
  )(f: Unmarshal[HttpResponse] => Future[A])(implicit ec: ExecutionContext, mat: Materializer): Future[A] = {
    val unmarshal = Unmarshal(response)
    if (response.status.isSuccess())
      f(unmarshal)
    else
      unmarshal.to[ErrorsResponseJson].flatMap { e =>
        Future.failed(new APIException(e.error_messages))
      }
  }
}
