package com.github.j5ik2o.threadWeaver.adaptor.json

final case class GetThreadsResponseJson(results: Seq[ThreadJson], error_messages: Seq[String] = Seq.empty)
    extends ResponseJson
