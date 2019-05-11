package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.http.json.JoinAdministratorIdsResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

private[adaptor] class AddAdministratorIdsPresenterImpl extends AddAdministratorIdsPresenter {

  override def response: Flow[AddAdministratorIdsResponse, JoinAdministratorIdsResponseJson, NotUsed] = {
    Flow[AddAdministratorIdsResponse].map {
      case f: AddAdministratorIdsFailed =>
        JoinAdministratorIdsResponseJson(threadId = None, error_messages = Seq(f.message))
      case s: AddAdministratorIdsSucceeded =>
        JoinAdministratorIdsResponseJson(threadId = Some(s.threadId.value.asString), error_messages = Seq.empty)
    }
  }

}
