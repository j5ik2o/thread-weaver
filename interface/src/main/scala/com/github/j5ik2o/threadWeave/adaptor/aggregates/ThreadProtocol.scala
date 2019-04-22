package com.github.j5ik2o.threadWeave.adaptor.aggregates

import java.time.Instant

import com.github.j5ik2o.threadWeave.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeave.infrastructure.ulid.ULID

object ThreadProtocol {

  sealed trait Message
  sealed trait Event extends Message {
    def id: ULID
    def threadId: ThreadId
  }
  sealed trait CommandMessage extends Message {
    def id: ULID
    def threadId: ThreadId
    def createdAt: Instant
  }
  sealed trait CommandRequest extends CommandMessage
  sealed trait CommandResponse extends CommandMessage {
    def requestId: ULID
  }

}
