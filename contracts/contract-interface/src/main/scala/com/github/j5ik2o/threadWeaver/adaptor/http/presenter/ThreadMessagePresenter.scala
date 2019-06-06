package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import com.github.j5ik2o.threadWeaver.adaptor.dao.ThreadMessageRecord
import com.github.j5ik2o.threadWeaver.adaptor.http.json.ThreadMessageJson
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter

trait ThreadMessagePresenter extends Presenter[ThreadMessageRecord, ThreadMessageJson]
