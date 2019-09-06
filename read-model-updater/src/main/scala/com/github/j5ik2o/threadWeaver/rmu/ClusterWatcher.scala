package com.github.j5ik2o.threadWeaver.rmu

import akka.actor.{ Actor, ActorLogging }
import akka.cluster.Cluster

class ClusterWatcher extends Actor with ActorLogging {
  private val cluster = Cluster(context.system)

  override def receive: PartialFunction[Any, Unit] = {
    case msg => log.info(s"Cluster ${cluster.selfAddress} >>> " + msg)
  }
}
