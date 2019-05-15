package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.{ DestroyThreadResponse => DestroyThreadGrpcResponse }
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.DestroyThreadResponse

trait DestroyThreadPresenter extends Presenter[DestroyThreadResponse, DestroyThreadGrpcResponse]
