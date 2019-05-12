package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import com.github.j5ik2o.threadWeaver.adaptor.http.presenter.Presenter
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.CreateThreadResponse
import com.github.j5ik2o.threadWeaver.adaptor.grpc.{ CreateThreadResponse => CreateThreadGrpcResponse }

trait CreateThreadPresenter extends Presenter[CreateThreadResponse, CreateThreadGrpcResponse]
