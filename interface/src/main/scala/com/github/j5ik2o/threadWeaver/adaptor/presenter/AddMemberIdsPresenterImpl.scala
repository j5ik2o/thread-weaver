package com.github.j5ik2o.threadWeaver.adaptor.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.json.AddMemberIdsResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

class AddMemberIdsPresenterImpl extends AddMemberIdsPresenter {

  override def response: Flow[AddMemberIdsResponse, AddMemberIdsResponseJson, NotUsed] = {
    Flow[AddMemberIdsResponse].map {
      case f: AddMemberIdsFailed =>
        AddMemberIdsResponseJson(id = None, error_messages = Seq(f.message))
      case s: AddMemberIdsSucceeded =>
        AddMemberIdsResponseJson(id = Some(s.threadId.value.asString), error_messages = Seq.empty)
    }
  }

}
