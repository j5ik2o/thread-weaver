package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import com.github.j5ik2o.threadWeaver.adaptor.dao.ThreadRecord
import com.github.j5ik2o.threadWeaver.adaptor.http.json.ThreadJson
import com.github.j5ik2o.threadWeaver.adaptor.presenter.Presenter

trait ThreadPresenter extends Presenter[ThreadRecord, ThreadJson]
