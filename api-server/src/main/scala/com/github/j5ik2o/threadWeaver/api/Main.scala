package com.github.j5ik2o.threadWeaver.api

import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ ActorSystem, Props }
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.{ Cluster, ClusterEvent }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import com.github.everpeace.healthchecks.k8s._
import com.github.j5ik2o.threadWeaver.adaptor.DISettings
import com.github.j5ik2o.threadWeaver.adaptor.http.routes.Routes
import com.typesafe.config.{ Config, ConfigFactory }
import kamon.Kamon
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContextExecutor }

object Main extends App {
  SLF4JBridgeHandler.install()
  Kamon.start()

  implicit val logger = LoggerFactory.getLogger(getClass)

  val envName        = sys.env.getOrElse("ENV_NAME", "development")
  val configResource = sys.env.get("CONFIG_RESOURCE")
  logger.info(s"ENV_NAME = $envName, configResource = $configResource")

  val config: Config = ConfigFactory.load()

  val journalTableName  = config.getString("dynamo-db-journal.table-name")
  val snapshotTableName = config.getString("dynamo-db-snapshot.table-name")

  logger.info(s"journalTableName = $journalTableName")
  logger.info(s"snapshotTableName = $snapshotTableName")

  implicit val system: ActorSystem                        = ActorSystem("thread-weaver-api-server", config)
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val cluster                                    = Cluster(system)
  logger.info(s"Started [$system], cluster.selfAddress = ${cluster.selfAddress}")

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  cluster.subscribe(
    system.actorOf(Props[ClusterWatcher]),
    ClusterEvent.InitialStateAsEvents,
    classOf[ClusterDomainEvent]
  )

  val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("slick", config)
  val profile  = dbConfig.profile
  val db       = dbConfig.db

  val clusterSharding = ClusterSharding(system.toTyped)

  val host = config.getString("thread-weaver.api-server.host")
  val port = config.getInt("thread-weaver.api-server.http.port")

  val akkaHealthCheck = HealthCheck.akka(host, port)

  val nrOfShardOfAggregates = config.getInt("thread-weaver.api-server.nr-of-shards")

  val design =
    DISettings.designOfAPI(
      host,
      port,
      system.toTyped,
      clusterSharding,
      materializer,
      profile,
      db,
      3 seconds,
      nrOfShardOfAggregates
    )
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
    val future = bindingFuture
      .flatMap { serverBinding =>
        session.shutdown
        Kamon.shutdown()
        materializer.shutdown()
        serverBinding.unbind()
      }.flatMap { _ =>
        system.terminate()
      }
    Await.result(future, config.getDuration("thread-weaver.api-server.terminate.duration").toMillis millis)
  }

}
