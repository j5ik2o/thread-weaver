package com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef

trait TypedActorSpecSupport {

  val testKit: ActorTestKit

  def killActors(actors: ActorRef[_]*): Unit = {
    actors.foreach { actor =>
      testKit.stop(actor)
      Thread.sleep(1000) // the actor name is not unique intermittently on travis when creating it again after killActors, this is ducktape.
    }
  }

}
