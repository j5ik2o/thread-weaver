package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import akka.actor.ActorRef
import akka.testkit.TestKit

trait ActorSpecSupport { this: TestKit =>

  def killActors(actors: ActorRef*): Unit = {
    actors.foreach { actor =>
      watch(actor)
      system.stop(actor)
      expectTerminated(actor)
      Thread.sleep(1000) // the actor name is not unique intermittently on travis when creating it again after killActors, this is ducktape.
    }
  }

}
