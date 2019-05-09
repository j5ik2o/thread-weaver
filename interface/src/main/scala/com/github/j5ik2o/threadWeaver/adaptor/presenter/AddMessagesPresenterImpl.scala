package com.github.j5ik2o.threadWeaver.adaptor.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.json.AddMessagesResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

private[adaptor] class AddMessagesPresenterImpl extends AddMessagesPresenter {

  override def response: Flow[AddMessagesResponse, AddMessagesResponseJson, NotUsed] = Flow[AddMessagesResponse].map {
    case f: AddMessagesFailed =>
      AddMessagesResponseJson(
        ids = Seq.empty,
        error_messages = Seq(f.message)
      )
    case s: AddMessagesSucceeded =>
      AddMessagesResponseJson(
        ids = s.messageIds.valuesAsString,
        error_messages = Seq.empty
      )
  }

}
