package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter
import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.RemoveMessagesResponse
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ RemoveMessagesFailed, RemoveMessagesSucceeded }

class RemoveMessagesPresenterImpl extends RemoveMessagesPresenter {
  override def response: Flow[ThreadWeaverProtocol.RemoveMessagesResponse, RemoveMessagesResponse, NotUsed] =
    Flow[ThreadWeaverProtocol.RemoveMessagesResponse].map {
      case s: RemoveMessagesSucceeded =>
        RemoveMessagesResponse(isSuccessful = true, s.messageIds.valuesAsString, Seq.empty)
      case f: RemoveMessagesFailed =>
        RemoveMessagesResponse(isSuccessful = false, Seq.empty, Seq(f.message))
    }
}
