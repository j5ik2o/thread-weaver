package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter
import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.AddMessagesResponse
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ AddMessagesFailed, AddMessagesSucceeded }

class AddMessagesPresenterImpl extends AddMessagesPresenter {
  override def response: Flow[ThreadWeaverProtocol.AddMessagesResponse, AddMessagesResponse, NotUsed] =
    Flow[ThreadWeaverProtocol.AddMessagesResponse].map {
      case s: AddMessagesSucceeded =>
        AddMessagesResponse(isSuccessful = true, s.messageIds.valuesAsString, Seq.empty)
      case f: AddMessagesFailed =>
        AddMessagesResponse(isSuccessful = false, Seq.empty, Seq(f.message))
    }
}
