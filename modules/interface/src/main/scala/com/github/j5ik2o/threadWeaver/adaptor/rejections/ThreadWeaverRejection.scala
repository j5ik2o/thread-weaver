package com.github.j5ik2o.threadWeaver.adaptor.rejections

import akka.http.javadsl.server.CustomRejection
import com.github.j5ik2o.threadWeaver.adaptor.error.InterfaceError

trait ThreadWeaverRejection extends CustomRejection {
  val message: String
  val cause: Option[InterfaceError]
  protected def withCauseMessage = s"$message${cause.fold("")(v => s": ${v.message}")}"
}
