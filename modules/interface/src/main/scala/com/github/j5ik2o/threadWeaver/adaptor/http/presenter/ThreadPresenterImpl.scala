package com.github.j5ik2o.threadWeaver.adaptor.http.presenter
import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.dao.ThreadRecord
import com.github.j5ik2o.threadWeaver.adaptor.http.json.ThreadJson

class ThreadPresenterImpl extends ThreadPresenter {

  override def response: Flow[ThreadRecord, ThreadJson, NotUsed] = Flow[ThreadRecord].map { threadRecord =>
    ThreadJson(
      threadRecord.id,
      threadRecord.creatorId,
      threadRecord.parentId,
      threadRecord.title,
      threadRecord.remarks,
      threadRecord.createdAt.toEpochMilli,
      threadRecord.updatedAt.toEpochMilli
    )
  }

}
