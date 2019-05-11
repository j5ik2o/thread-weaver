package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import com.github.j5ik2o.threadWeaver.adaptor.http.json.AddMessagesResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.AddMessagesResponse

trait AddMessagesPresenter extends Presenter[AddMessagesResponse, AddMessagesResponseJson]
