package com.github.j5ik2o.threadWeaver.api

import akka.http.scaladsl.model.Uri

final case class ClientSettings(
    scheme: String,
    host: String,
    port: Option[Int] = None,
    version: String = "v1",
    queueSize: Int,
    https: Boolean = false
) {
  def urlString: String = s"$scheme://$host${port.fold("")(p => s":$p")}"
  def uri: Uri          = Uri.from(scheme = scheme, host = host, port = port.getOrElse(0))
}
