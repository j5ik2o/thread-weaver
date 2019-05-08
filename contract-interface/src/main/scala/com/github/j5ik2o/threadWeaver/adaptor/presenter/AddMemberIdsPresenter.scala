package com.github.j5ik2o.threadWeaver.adaptor.presenter

import com.github.j5ik2o.threadWeaver.adaptor.json.AddMemberIdsResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.AddMemberIdsResponse

trait AddMemberIdsPresenter extends Presenter[AddMemberIdsResponse, AddMemberIdsResponseJson]
