package com.github.j5ik2o.threadWeaver.adaptor.routing

import java.time.Instant

import akka.actor.{ Actor, ActorRef, Props }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadCommonProtocol
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.{ ThreadReadModelUpdaterProtocol, ThreadTag }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object ThreadToRMURouter {

  def props(rmu: ActorRef): Props = Props(new ThreadToRMURouter(rmu))

}

class ThreadToRMURouter(rmu: ActorRef) extends Actor {
  implicit val config = context.system.settings.config

  override def receive: Receive = {
    case msg: ThreadCommonProtocol.Started =>
      rmu ! ThreadReadModelUpdaterProtocol.Start(ULID(), ThreadTag.fromThreadId(msg.threadId), Instant.now)
    case msg: ThreadCommonProtocol.Stopped =>
      rmu ! ThreadReadModelUpdaterProtocol.Stop(ULID(), ThreadTag.fromThreadId(msg.threadId), Instant.now)
  }
}
