package com.github.j5ik2o.threadWeave.adaptor.aggregates
import akka.actor.typed.ActorRef
import com.github.j5ik2o.threadWeave.domain.model.threads.ThreadId

class ThreadAggregatesSpec extends ThreadAggregateSpec {
  override def newThreadRef(threadId: ThreadId): ActorRef[ThreadProtocol.CommandRequest] =
    spawn(ThreadAggregates.behavior(ThreadAggregate.name)(ThreadAggregate.behavior))

}
