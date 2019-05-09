package com.github.j5ik2o.threadWeaver.adaptor.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.json.AddMessagesResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  AddMessagesFailed,
  AddMessagesResponse,
  AddMessagesSucceeded
}

class AddMessagesPresenterImpl extends AddMessagesPresenter {
  override def response: Flow[AddMessagesResponse, AddMessagesResponseJson, NotUsed] = Flow[AddMessagesResponse].map {
    case f: AddMessagesFailed =>
      AddMessagesResponseJson(
        id = None,
        error_messages = Seq(f.message)
      )
    case s: AddMessagesSucceeded =>
      AddMessagesResponseJson(
        id = Some(s.threadId.value.asString),
        error_messages = Seq.empty
      )

  }
}
