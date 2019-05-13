package com.github.j5ik2o.threadWeaver.adaptor.http.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.http.json.JoinAdministratorIdsResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

private[adaptor] class JoinAdministratorIdsPresenterImpl extends JoinAdministratorIdsPresenter {

  override def response: Flow[JoinAdministratorIdsResponse, JoinAdministratorIdsResponseJson, NotUsed] = {
    Flow[JoinAdministratorIdsResponse].map {
      case f: JoinAdministratorIdsFailed =>
        JoinAdministratorIdsResponseJson(threadId = None, error_messages = Seq(f.message))
      case s: JoinAdministratorIdsSucceeded =>
        JoinAdministratorIdsResponseJson(threadId = Some(s.threadId.value.asString), error_messages = Seq.empty)
    }
  }

}
