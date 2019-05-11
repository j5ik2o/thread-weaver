package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import akka.actor.typed.ActorRef
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId

class ThreadAggregatesSpec extends ThreadAggregateSpec {

  override def newThreadRef(threadId: ThreadId): ActorRef[ThreadProtocol.CommandRequest] =
    spawn(ThreadAggregates.behavior(Seq.empty, ThreadAggregate.name)(ThreadAggregate.behavior))

}
