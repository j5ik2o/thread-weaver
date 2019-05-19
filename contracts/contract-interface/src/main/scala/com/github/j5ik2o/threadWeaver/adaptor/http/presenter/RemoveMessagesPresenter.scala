package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import com.github.j5ik2o.threadWeaver.adaptor.http.json.RemoveMessagesResponseJson
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.RemoveMessagesResponse

trait RemoveMessagesPresenter extends Presenter[RemoveMessagesResponse, RemoveMessagesResponseJson]
