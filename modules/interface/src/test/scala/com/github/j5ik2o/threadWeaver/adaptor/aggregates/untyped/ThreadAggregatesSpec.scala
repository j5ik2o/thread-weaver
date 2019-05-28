package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped
import akka.actor.ActorRef
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId

class ThreadAggregatesSpec extends ThreadAggregateSpec {
  override def newThreadRef(threadId: ThreadId): ActorRef =
    system.actorOf(ThreadAggregates.props(Seq.empty, ThreadAggregate.props))
}
