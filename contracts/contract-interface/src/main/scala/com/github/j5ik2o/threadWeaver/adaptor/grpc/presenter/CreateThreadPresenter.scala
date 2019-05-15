package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.CreateThreadResponse
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.{ CreateThreadResponse => CreateThreadGrpcResponse }
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter

trait CreateThreadPresenter extends Presenter[CreateThreadResponse, CreateThreadGrpcResponse]
