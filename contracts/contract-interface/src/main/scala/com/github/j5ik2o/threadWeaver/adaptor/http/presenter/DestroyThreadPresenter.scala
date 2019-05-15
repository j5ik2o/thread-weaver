package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import com.github.j5ik2o.threadWeaver.adaptor.http.json.DestroyThreadResponseJson
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.DestroyThreadResponse

trait DestroyThreadPresenter extends Presenter[DestroyThreadResponse, DestroyThreadResponseJson]
