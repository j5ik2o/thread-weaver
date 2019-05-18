package com.github.j5ik2o.threadWeaver.api

import java.net.URL

import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ ActorSystem, Props }
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.{ Cluster, ClusterEvent }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import com.github.everpeace.healthchecks.k8s._
import com.github.j5ik2o.akka.persistence.dynamodb.query.scaladsl.DynamoDBReadJournal
import com.github.j5ik2o.threadWeaver.adaptor.DISettings
import com.github.j5ik2o.threadWeaver.adaptor.http.routes.Routes
import com.github.j5ik2o.threadWeaver.api.config.EnvironmentURLStreamHandlerFactory
import com.typesafe.config.{ Config, ConfigFactory }
import kamon.Kamon
import kamon.datadog.DatadogAgentReporter
import kamon.jmx.collector.KamonJmxMetricCollector
import kamon.system.SystemMetrics
import org.slf4j.LoggerFactory
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

object Main extends App {

//  SLF4JBridgeHandler.install()
  val logger = LoggerFactory.getLogger(getClass)

  val envName = sys.env.getOrElse("ENV_NAME", "development")
  logger.info(s"ENV_NAME = $envName")

  URL.setURLStreamHandlerFactory(new EnvironmentURLStreamHandlerFactory)
  System.setProperty("environment", envName)

  val config: Config = ConfigFactory.load()

  implicit val system           = ActorSystem("thread-weaver-api", config)
  implicit val materializer     = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  if (envName == "development")
    Kamon.addReporter(LogReporter)
  else
    Kamon.addReporter(new DatadogAgentReporter())

  SystemMetrics.startCollecting()

  KamonJmxMetricCollector()

  implicit val cluster: Cluster = Cluster(system)

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  cluster.subscribe(
    system.actorOf(Props[ClusterWatcher]),
    ClusterEvent.InitialStateAsEvents,
    classOf[ClusterDomainEvent]
  )

  val readJournal = PersistenceQuery(system).readJournalFor[DynamoDBReadJournal](DynamoDBReadJournal.Identifier)
  val dbConfig    = DatabaseConfig.forConfig[JdbcProfile]("slick", config)
  val profile     = dbConfig.profile
  val db          = dbConfig.db

  val clusterSharding = ClusterSharding(system.toTyped)

  val host = config.getString("thread-weaver.api.host")
  val port = config.getInt("thread-weaver.api.port")

  val akkaHealthCheck = HealthCheck.akka(host, port)

  val design =
    DISettings.design(host, port, system.toTyped, clusterSharding, materializer, readJournal, profile, db)
  val session = design.newSession
  session.start

  val routes = session
    .build[Routes].root ~ readinessProbe(akkaHealthCheck).toRoute ~ livenessProbe(akkaHealthCheck).toRoute

  val bindingFuture = Http().bindAndHandle(routes, host, port).map { serverBinding =>
    system.log.info(s"Server online at ${serverBinding.localAddress}")
    serverBinding
  }

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
