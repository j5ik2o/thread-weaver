package com.github.j5ik2o.threadWeaver.adaptor.json

case class CreateThreadResponseJson(id: Option[String], error_messages: Seq[String] = Seq.empty) extends ResponseJson
