package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.http.json.JoinMemberIdsResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

private[adaptor] class JoinMemberIdsPresenterImpl extends JoinMemberIdsPresenter {

  override def response: Flow[JoinMemberIdsResponse, JoinMemberIdsResponseJson, NotUsed] = {
    Flow[JoinMemberIdsResponse].map {
      case f: JoinMemberIdsFailed =>
        JoinMemberIdsResponseJson(threadId = None, error_messages = Seq(f.message))
      case s: JoinMemberIdsSucceeded =>
        JoinMemberIdsResponseJson(threadId = Some(s.threadId.value.asString), error_messages = Seq.empty)
    }
  }

}
