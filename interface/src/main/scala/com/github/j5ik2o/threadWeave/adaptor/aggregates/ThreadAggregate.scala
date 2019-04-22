package com.github.j5ik2o.threadWeave.adaptor.aggregates

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.github.j5ik2o.threadWeave.adaptor.aggregates.ThreadProtocol.CommandRequest

object ThreadAggregate {

  def behavior(): Behavior[CommandRequest] = Behaviors.setup[CommandRequest] { ctx =>
    Behaviors.same
  }

}
