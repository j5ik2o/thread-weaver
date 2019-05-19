package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.LeaveMemberIdsResponse
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ LeaveMemberIdsFailed, LeaveMemberIdsSucceeded }

class LeaveMemberIdsPresenterImpl extends LeaveMemberIdsPresenter {
  override def response: Flow[ThreadWeaverProtocol.LeaveMemberIdsResponse, LeaveMemberIdsResponse, NotUsed] =
    Flow[ThreadWeaverProtocol.LeaveMemberIdsResponse].map {
      case s: LeaveMemberIdsSucceeded =>
        LeaveMemberIdsResponse(isSuccessful = true, s.threadId.value.asString, Seq.empty)
      case f: LeaveMemberIdsFailed =>
        LeaveMemberIdsResponse(isSuccessful = false, "", Seq(f.message))
    }
}
