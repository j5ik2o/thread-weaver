package com.github.j5ik2o.threadWeaver.adaptor.grpc

import scala.concurrent.Future

class ThreadsServiceImpl extends ThreadsService {
  override def createThread(in: CreateThreadRequest): Future[CreateThreadResponse]    = ???
  override def destroyThread(in: DestroyThreadRequest): Future[DestroyThreadResponse] = ???
  override def addMessages(in: AddMessagesRequest): Future[AddMessagesResponse]       = ???
}
