package com.github.j5ik2o.threadWeaver.adaptor.http.routes

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.directives.LogEntry

trait RequestFormatter {
  def formatRequest(request: HttpRequest): LogEntry
}
