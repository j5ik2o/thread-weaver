package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.http.json.JoinMemberIdsResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

private[adaptor] class AddMemberIdsPresenterImpl extends AddMemberIdsPresenter {

  override def response: Flow[AddMemberIdsResponse, JoinMemberIdsResponseJson, NotUsed] = {
    Flow[AddMemberIdsResponse].map {
      case f: AddMemberIdsFailed =>
        JoinMemberIdsResponseJson(threadId = None, error_messages = Seq(f.message))
      case s: AddMemberIdsSucceeded =>
        JoinMemberIdsResponseJson(threadId = Some(s.threadId.value.asString), error_messages = Seq.empty)
    }
  }

}
