package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter
import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.DestroyThreadResponse
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ DestroyThreadFailed, DestroyThreadSucceeded }

class DestroyThreadPresenterImpl extends DestroyThreadPresenter {
  override def response: Flow[ThreadWeaverProtocol.DestroyThreadResponse, DestroyThreadResponse, NotUsed] =
    Flow[ThreadWeaverProtocol.DestroyThreadResponse].map {
      case s: DestroyThreadSucceeded =>
        DestroyThreadResponse(isSuccessful = true, s.threadId.value.asString, Seq.empty)
      case f: DestroyThreadFailed =>
        DestroyThreadResponse(isSuccessful = false, "", Seq(f.message))
    }
}
