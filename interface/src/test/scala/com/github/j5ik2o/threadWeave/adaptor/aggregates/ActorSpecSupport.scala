package com.github.j5ik2o.threadWeave.adaptor.aggregates

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef

trait ActorSpecSupport {

  val testKit: ActorTestKit

  def killActors(actors: ActorRef[_]*): Unit = {
    actors.foreach { actor =>
      testKit.stop(actor)
      Thread.sleep(1000) // the actor name is not unique intermittently on travis when creating it again after killActors, this is ducktape.
    }
  }

}
