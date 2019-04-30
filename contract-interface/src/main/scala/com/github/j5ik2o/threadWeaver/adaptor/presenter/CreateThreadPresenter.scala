package com.github.j5ik2o.threadWeaver.adaptor.presenter

import com.github.j5ik2o.threadWeaver.adaptor.json.CreateThreadResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.CreateThreadResponse

trait CreateThreadPresenter extends Presenter[CreateThreadResponse, CreateThreadResponseJson]
