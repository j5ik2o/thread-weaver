package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.JoinAdministratorIdsResponse
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.{
  JoinAdministratorIdsResponse => JoinAdministratorIdsGrpcResponse
}
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter

trait JoinAdministratorIdsPresenter extends Presenter[JoinAdministratorIdsResponse, JoinAdministratorIdsGrpcResponse]
