package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.JoinAdministratorIdsResponse
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  JoinAdministratorIdsFailed,
  JoinAdministratorIdsSucceeded
}

class JoinAdministratorIdsPresenterImpl extends JoinAdministratorIdsPresenter {
  override def response
      : Flow[ThreadWeaverProtocol.JoinAdministratorIdsResponse, JoinAdministratorIdsResponse, NotUsed] =
    Flow[ThreadWeaverProtocol.JoinAdministratorIdsResponse].map {
      case s: JoinAdministratorIdsSucceeded =>
        JoinAdministratorIdsResponse(isSuccessful = true, s.threadId.value.asString, Seq.empty)
      case f: JoinAdministratorIdsFailed =>
        JoinAdministratorIdsResponse(isSuccessful = false, "", Seq(f.message))
    }
}
