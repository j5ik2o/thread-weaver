package com.github.j5ik2o.threadWeaver.api

import com.typesafe.config.Config
import kamon.MetricReporter
import kamon.metric.PeriodSnapshot
import org.slf4j.LoggerFactory

object LogReporter extends MetricReporter {
  val logger = LoggerFactory.getLogger("log-reporter")

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
    logger.info(snapshot.toString)
  }

  override def start(): Unit = {
    logger.info("start")
  }

  override def stop(): Unit = {
    logger.info("stop")
  }

  override def reconfigure(config: Config): Unit = {
    logger.info("reconfigure: {}", config)
  }

}
