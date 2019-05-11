package com.github.j5ik2o.threadWeaver.useCase

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ ThreadWeaverRequest, ThreadWeaverResponse }

trait ThreadWeaverUseCase[Req <: ThreadWeaverRequest, Res <: ThreadWeaverResponse] {
  def execute: Flow[Req, Res, NotUsed]
}
