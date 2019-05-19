package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.JoinMemberIdsResponse
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.{ JoinMemberIdsResponse => JoinMemberIdsGrpcResponse }
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter

trait JoinMemberIdsPresenter extends Presenter[JoinMemberIdsResponse, JoinMemberIdsGrpcResponse]
