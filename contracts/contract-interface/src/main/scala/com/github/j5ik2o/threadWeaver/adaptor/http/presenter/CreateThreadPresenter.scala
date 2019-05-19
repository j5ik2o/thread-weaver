package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import com.github.j5ik2o.threadWeaver.adaptor.http.json.CreateThreadResponseJson
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.CreateThreadResponse

trait CreateThreadPresenter extends Presenter[CreateThreadResponse, CreateThreadResponseJson]
