package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object ThreadCommonProtocol {
  trait Message

  trait Event extends Message {
    def id: ULID
    def threadId: ThreadId
    def createdAt: Instant
  }

  case class Started(id: ULID, threadId: ThreadId, createdAt: Instant) extends Event

  case class Stopped(id: ULID, threadId: ThreadId, createdAt: Instant) extends Event
}
