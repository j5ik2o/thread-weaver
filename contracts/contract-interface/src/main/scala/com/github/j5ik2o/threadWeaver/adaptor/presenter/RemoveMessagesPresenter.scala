package com.github.j5ik2o.threadWeaver.adaptor.presenter

import com.github.j5ik2o.threadWeaver.adaptor.json.RemoveMessagesResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.RemoveMessagesResponse

trait RemoveMessagesPresenter extends Presenter[RemoveMessagesResponse, RemoveMessagesResponseJson]
