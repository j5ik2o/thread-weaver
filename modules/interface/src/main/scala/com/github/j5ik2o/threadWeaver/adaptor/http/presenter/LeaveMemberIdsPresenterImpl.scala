package com.github.j5ik2o.threadWeaver.adaptor.http.presenter
import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.http.json.LeaveMemberIdsResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol._

private[adaptor] class LeaveMemberIdsPresenterImpl extends LeaveMemberIdsPresenter {

  override def response: Flow[LeaveMemberIdsResponse, LeaveMemberIdsResponseJson, NotUsed] =
    Flow[LeaveMemberIdsResponse].map {
      case f: LeaveMemberIdsFailed =>
        LeaveMemberIdsResponseJson(threadId = None, error_messages = Seq(f.message))
      case s: LeaveMemberIdsSucceeded =>
        LeaveMemberIdsResponseJson(threadId = Some(s.threadId.value.asString), error_messages = Seq.empty)
    }

}
