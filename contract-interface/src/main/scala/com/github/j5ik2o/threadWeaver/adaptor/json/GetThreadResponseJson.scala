package com.github.j5ik2o.threadWeaver.adaptor.json

final case class ThreadJson(id: String)

final case class GetThreadResponseJson(result: Seq[ThreadJson], error_messages: Seq[String] = Seq.empty)
    extends ResponseJson
