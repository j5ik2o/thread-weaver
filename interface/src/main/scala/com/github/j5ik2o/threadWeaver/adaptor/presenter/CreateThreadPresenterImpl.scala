package com.github.j5ik2o.threadWeaver.adaptor.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.json.CreateThreadResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

private[adaptor] class CreateThreadPresenterImpl extends CreateThreadPresenter {

  override def response: Flow[CreateThreadResponse, CreateThreadResponseJson, NotUsed] =
    Flow[CreateThreadResponse].map {
      case f: CreateThreadFailed =>
        CreateThreadResponseJson(
          threadId = None,
          error_messages = Seq(f.message)
        )
      case s: CreateThreadSucceeded =>
        CreateThreadResponseJson(
          threadId = Some(s.threadId.value.asString),
          error_messages = Seq.empty
        )
    }

}
