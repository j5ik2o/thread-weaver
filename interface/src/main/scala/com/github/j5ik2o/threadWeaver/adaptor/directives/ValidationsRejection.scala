package com.github.j5ik2o.threadWeaver.adaptor.directives

import akka.http.javadsl.server.CustomRejection
import cats.data.NonEmptyList
import com.github.j5ik2o.threadWeaver.adaptor.error.InterfaceError

case class ValidationsRejection(errors: NonEmptyList[InterfaceError]) extends CustomRejection
