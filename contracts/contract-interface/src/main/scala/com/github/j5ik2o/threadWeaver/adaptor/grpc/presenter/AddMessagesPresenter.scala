package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.AddMessagesResponse
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.{ AddMessagesResponse => AddMessagesGrpcResponse }

trait AddMessagesPresenter extends Presenter[AddMessagesResponse, AddMessagesGrpcResponse]
