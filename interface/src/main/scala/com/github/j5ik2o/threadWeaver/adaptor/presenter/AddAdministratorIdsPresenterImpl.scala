package com.github.j5ik2o.threadWeaver.adaptor.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.json.AddAdministratorIdsResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

class AddAdministratorIdsPresenterImpl extends AddAdministratorIdsPresenter {

  override def response: Flow[AddAdministratorIdsResponse, AddAdministratorIdsResponseJson, NotUsed] = {
    Flow[AddAdministratorIdsResponse].map {
      case f: AddAdministratorIdsFailed =>
        AddAdministratorIdsResponseJson(id = None, error_messages = Seq(f.message))
      case s: AddAdministratorIdsSucceeded =>
        AddAdministratorIdsResponseJson(id = Some(s.threadId.value.asString), error_messages = Seq.empty)
    }
  }

}
