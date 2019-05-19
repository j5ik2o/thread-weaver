package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.{ LeaveMemberIdsResponse => LeaveMemberIdsGrpcResponse }
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.LeaveMemberIdsResponse

trait LeaveMemberIdsPresenter extends Presenter[LeaveMemberIdsResponse, LeaveMemberIdsGrpcResponse]
