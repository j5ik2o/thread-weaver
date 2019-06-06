package com.github.j5ik2o.threadWeaver.adaptor.http.presenter
import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.dao.ThreadMessageRecord
import com.github.j5ik2o.threadWeaver.adaptor.http.json.ThreadMessageJson

class ThreadMessagePresenterImpl extends ThreadMessagePresenter {
  override def response: Flow[ThreadMessageRecord, ThreadMessageJson, NotUsed] = Flow[ThreadMessageRecord].map {
    messageRecord =>
      ThreadMessageJson(
        messageRecord.id,
        messageRecord.threadId,
        messageRecord.senderId,
        messageRecord.`type`,
        messageRecord.body,
        messageRecord.createdAt.toEpochMilli,
        messageRecord.updatedAt.toEpochMilli
      )
  }
}
