package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.http.json.DestroyThreadResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  DestroyThreadFailed,
  DestroyThreadResponse,
  DestroyThreadSucceeded
}

class DestroyThreadPresenterImpl extends DestroyThreadPresenter {

  override def response: Flow[DestroyThreadResponse, DestroyThreadResponseJson, NotUsed] =
    Flow[DestroyThreadResponse].map {
      case f: DestroyThreadFailed =>
        DestroyThreadResponseJson(
          threadId = None,
          error_messages = Seq(f.message)
        )
      case s: DestroyThreadSucceeded =>
        DestroyThreadResponseJson(
          threadId = Some(s.threadId.value.asString),
          error_messages = Seq.empty
        )
    }

}
