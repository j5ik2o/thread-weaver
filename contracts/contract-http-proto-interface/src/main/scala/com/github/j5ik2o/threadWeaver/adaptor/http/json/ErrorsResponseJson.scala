package com.github.j5ik2o.threadWeaver.adaptor.http.json

final case class ErrorsResponseJson(error_messages: Seq[String]) extends ResponseJson
