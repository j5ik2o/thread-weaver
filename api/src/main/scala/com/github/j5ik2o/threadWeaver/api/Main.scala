package com.github.j5ik2o.threadWeaver.api

import java.net.URL

import akka.actor.{ ActorSystem, Props }
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.{ Cluster, ClusterEvent }
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import com.github.j5ik2o.threadWeaver.adaptor.AirframeSettings
import com.github.j5ik2o.threadWeaver.adaptor.routes.Routes
import com.github.j5ik2o.threadWeaver.api.config.EnvironmentURLStreamHandlerFactory
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.datadog.DatadogAgentReporter
import kamon.jmx.collector.KamonJmxMetricCollector
import kamon.system.SystemMetrics
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

object Main extends App {
  SLF4JBridgeHandler.install()

  URL.setURLStreamHandlerFactory(new EnvironmentURLStreamHandlerFactory)
  System.setProperty("environment", args.headOption.getOrElse("development"))

  lazy val config = ConfigFactory.load()
  val devMode     = sys.env.get("ENV_NAME").contains("DEV")

  val logger = LoggerFactory.getLogger(getClass)

  logger.info(s"devMode = $devMode")
  if (devMode)
    Kamon.addReporter(LogReporter)
  else
    Kamon.addReporter(new DatadogAgentReporter())

  SystemMetrics.startCollecting()

  implicit val system           = ActorSystem("thread-weaver-api", config)
  implicit val materializer     = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  KamonJmxMetricCollector()

  implicit val cluster = Cluster(system)

  val akkaManagement   = AkkaManagement(system)
  val clusterBootstrap = ClusterBootstrap(system)

  akkaManagement.start()
  clusterBootstrap.start()

  cluster.subscribe(
    system.actorOf(Props[ClusterWatcher]),
    ClusterEvent.InitialStateAsEvents,
    classOf[ClusterDomainEvent]
  )

  val clusterSharding = ClusterSharding(system.toTyped)

  val host = config.getString("thread-weaver.api.host")
  val port = config.getInt("thread-weaver.api.port")

  val design  = AirframeSettings.design(host, port, system.toTyped, clusterSharding, materializer)
  val session = design.newSession
  session.start

  val routes        = session.build[Routes]
  val bindingFuture = Http().bindAndHandle(routes.root, host, port)

  Cluster(system).registerOnMemberUp({
    logger.info("Cluster member is up!")
  })

  sys.addShutdownHook {
    SystemMetrics.stopCollecting()
    Kamon.stopAllReporters()
    Kamon.scheduler().shutdown()
    session.shutdown
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

}
