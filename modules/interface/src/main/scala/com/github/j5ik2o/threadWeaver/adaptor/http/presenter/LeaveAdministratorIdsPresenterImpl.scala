package com.github.j5ik2o.threadWeaver.adaptor.http.presenter
import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.http.json.LeaveAdministratorIdsResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

private[adaptor] class LeaveAdministratorIdsPresenterImpl extends LeaveAdministratorIdsPresenter {

  override def response: Flow[LeaveAdministratorIdsResponse, LeaveAdministratorIdsResponseJson, NotUsed] =
    Flow[LeaveAdministratorIdsResponse].map {
      case f: LeaveAdministratorIdsFailed =>
        LeaveAdministratorIdsResponseJson(threadId = None, error_messages = Seq(f.message))
      case s: LeaveAdministratorIdsSucceeded =>
        LeaveAdministratorIdsResponseJson(threadId = Some(s.threadId.value.asString), error_messages = Seq.empty)
    }

}
