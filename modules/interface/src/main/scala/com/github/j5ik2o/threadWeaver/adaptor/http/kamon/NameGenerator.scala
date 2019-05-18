package com.github.j5ik2o.threadWeaver.adaptor.http.kamon

import akka.http.scaladsl.model.HttpRequest
import kamon.akka.http.AkkaHttp.OperationNameGenerator

class NameGenerator extends OperationNameGenerator {
  override def serverOperationName(request: HttpRequest): String = request.getUri().path()

  override def clientOperationName(request: HttpRequest): String = "request-level " + request.uri.path.toString()
}
