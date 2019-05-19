package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.LeaveAdministratorIdsResponse
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  LeaveAdministratorIdsFailed,
  LeaveAdministratorIdsSucceeded
}

class LeaveAdministratorIdsPresenterImpl extends LeaveAdministratorIdsPresenter {
  override def response
      : Flow[ThreadWeaverProtocol.LeaveAdministratorIdsResponse, LeaveAdministratorIdsResponse, NotUsed] =
    Flow[ThreadWeaverProtocol.LeaveAdministratorIdsResponse].map {
      case s: LeaveAdministratorIdsSucceeded =>
        LeaveAdministratorIdsResponse(isSuccessful = true, s.threadId.value.asString, Seq.empty)
      case f: LeaveAdministratorIdsFailed =>
        LeaveAdministratorIdsResponse(isSuccessful = false, "", Seq(f.message))
    }
}
