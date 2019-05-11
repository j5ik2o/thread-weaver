package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.http.json.RemoveMessagesResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

private[adaptor] class RemoveMessagesPresenterImpl extends RemoveMessagesPresenter {

  override def response: Flow[RemoveMessagesResponse, RemoveMessagesResponseJson, NotUsed] =
    Flow[RemoveMessagesResponse].map {
      case f: RemoveMessagesFailed =>
        RemoveMessagesResponseJson(
          messageIds = Seq.empty,
          error_messages = Seq(f.message)
        )
      case s: RemoveMessagesSucceeded =>
        RemoveMessagesResponseJson(
          messageIds = s.messageIds.valuesAsString,
          error_messages = Seq.empty
        )
    }

}
