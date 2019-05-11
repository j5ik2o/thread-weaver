package com.github.j5ik2o.threadWeaver.adaptor.grpc

import scala.concurrent.Future

class ThreadsServiceImpl extends ThreadsService {
  override def newThread(in: NewThreadRequest): Future[NewThreadResponse] = ???
}
