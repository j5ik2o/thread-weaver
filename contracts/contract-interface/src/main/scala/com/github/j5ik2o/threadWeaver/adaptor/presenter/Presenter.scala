package com.github.j5ik2o.threadWeaver.adaptor.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.json.ResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.ThreadWeaverResponse

trait Presenter[Res <: ThreadWeaverResponse, Json <: ResponseJson] {
  def response: Flow[Res, Json, NotUsed]
}
