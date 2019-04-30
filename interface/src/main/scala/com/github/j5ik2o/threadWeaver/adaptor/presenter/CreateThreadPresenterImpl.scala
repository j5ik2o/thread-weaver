package com.github.j5ik2o.threadWeaver.adaptor.presenter
import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.adaptor.json.CreateThreadResponseJson
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol

class CreateThreadPresenterImpl extends CreateThreadPresenter {
  override def response: Flow[ThreadWeaverProtocol.CreateThreadResponse, CreateThreadResponseJson, NotUsed] = ???
}
