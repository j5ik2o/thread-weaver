package com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter

import com.github.j5ik2o.threadWeaver.adaptor.grpc.model.{ RemoveMessagesResponse => RemoveMessagesGrpcResponse }
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.RemoveMessagesResponse

trait RemoveMessagesPresenter extends Presenter[RemoveMessagesResponse, RemoveMessagesGrpcResponse]
