package com.github.j5ik2o.threadWeaver.adaptor.json

final case class ValidationErrorsResponseJson(error_messages: Seq[String]) extends ResponseJson
