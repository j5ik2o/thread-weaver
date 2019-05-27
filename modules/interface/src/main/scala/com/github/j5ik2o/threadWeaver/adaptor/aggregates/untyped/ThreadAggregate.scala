package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import akka.actor.{Actor, ActorRef, Props}
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.threads.{Messages, Thread, ThreadId}
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object ThreadAggregate {

  def props(id: ThreadId, subscribers: Seq[ActorRef]): Props = Props(new ThreadAggregate(id, subscribers))

}

class ThreadAggregate(id: ThreadId, subscribers: Seq[ActorRef]) extends Actor {
  def onCreated(thread: Thread): Receive = ???

  override def receive: Receive = {
    case ExistsThread(requestId, threadId, _, createAt) if threadId == id =>
      sender() ! ExistsThreadSucceeded(ULID(), requestId, threadId, exists = false, createAt)
    case CreateThread(
    requestId,
    threadId,
    creatorId,
    parentThreadId,
    title,
    remarks,
    administratorIds,
    memberIds,
    createAt,
    reply
    ) if threadId == id =>
      if (reply)
        sender() ! CreateThreadSucceeded(ULID(), requestId, threadId, createAt)
      onCreated(
        Thread(
          threadId,
          creatorId,
          parentThreadId,
          title,
          remarks,
          administratorIds,
          memberIds,
          Messages.empty,
          createAt,
          createAt
        )
      )
    case DestroyThread(requestId, threadId, _, createAt, replyTo) if threadId == id =>
      if (replyTo)
        sender() ! DestroyThreadFailed(ULID(), requestId, threadId, "Not created yet", createAt))
  }
}
