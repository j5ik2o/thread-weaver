package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.CreateThreadResponse
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ CreateThreadFailed, CreateThreadSucceeded }

class CreateThreadPresenterImpl extends CreateThreadPresenter {
  override def response: Flow[ThreadWeaverProtocol.CreateThreadResponse, CreateThreadResponse, NotUsed] =
    Flow[ThreadWeaverProtocol.CreateThreadResponse].map {
      case s: CreateThreadSucceeded =>
        CreateThreadResponse(isSuccessful = true, s.threadId.value.asString, Seq.empty)
      case f: CreateThreadFailed =>
        CreateThreadResponse(isSuccessful = false, "", Seq(f.message))
    }
}
