package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.LeaveAdministratorIdsResponse
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.{
  LeaveAdministratorIdsResponse => LeaveAdministratorIdsGrpcResponse
}
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter

trait LeaveAdministratorIdsPresenter extends Presenter[LeaveAdministratorIdsResponse, LeaveAdministratorIdsGrpcResponse]
