package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.JoinMemberIdsResponse
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ JoinMemberIdsFailed, JoinMemberIdsSucceeded }

class JoinMemberIdsPresenterImpl extends JoinMemberIdsPresenter {
  override def response: Flow[ThreadWeaverProtocol.JoinMemberIdsResponse, JoinMemberIdsResponse, NotUsed] =
    Flow[ThreadWeaverProtocol.JoinMemberIdsResponse].map {
      case s: JoinMemberIdsSucceeded =>
        JoinMemberIdsResponse(isSuccessful = true, s.threadId.value.asString, Seq.empty)
      case f: JoinMemberIdsFailed =>
        JoinMemberIdsResponse(isSuccessful = false, "", Seq(f.message))
    }
}
