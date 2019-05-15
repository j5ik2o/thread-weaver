package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import com.github.j5ik2o.threadWeaver.adaptor.http.json.LeaveMemberIdsResponseJson
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.LeaveMemberIdsResponse

trait LeaveMemberIdsPresenter extends Presenter[LeaveMemberIdsResponse, LeaveMemberIdsResponseJson]
