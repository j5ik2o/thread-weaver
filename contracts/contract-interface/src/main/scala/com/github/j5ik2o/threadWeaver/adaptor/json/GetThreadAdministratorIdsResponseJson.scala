package com.github.j5ik2o.threadWeaver.adaptor.json

final case class GetThreadAdministratorIdsResponseJson(results: Seq[String], error_messages: Seq[String] = Seq.empty)
    extends ResponseJson
