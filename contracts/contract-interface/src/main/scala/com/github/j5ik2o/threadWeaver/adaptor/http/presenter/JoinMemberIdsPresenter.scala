package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import com.github.j5ik2o.threadWeaver.adaptor.http.json.JoinMemberIdsResponseJson
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.JoinMemberIdsResponse

trait JoinMemberIdsPresenter extends Presenter[JoinMemberIdsResponse, JoinMemberIdsResponseJson]
