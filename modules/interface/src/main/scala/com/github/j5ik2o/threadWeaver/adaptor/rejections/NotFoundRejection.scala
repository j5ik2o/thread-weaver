package com.github.j5ik2o.threadWeaver.adaptor.rejections
import com.github.j5ik2o.threadWeaver.adaptor.error.InterfaceError

case class NotFoundRejection(override val message: String, override val cause: Option[InterfaceError])
    extends ThreadWeaverRejection
