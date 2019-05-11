package com.github.j5ik2o.threadWeaver.adaptor.http.directives

import cats.data.NonEmptyList
import com.github.j5ik2o.threadWeaver.adaptor.error.InterfaceError
import com.github.j5ik2o.threadWeaver.adaptor.http.rejections.ThreadWeaverRejection

case class ValidationsRejection(errors: NonEmptyList[InterfaceError]) extends ThreadWeaverRejection {
  override val message: String               = errors.toList.map(_.message).mkString(",")
  override val cause: Option[InterfaceError] = None
}
