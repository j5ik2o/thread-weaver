package com.github.j5ik2o.threadWeaver.api.config
import java.io.InputStream
import java.net.{ URL, URLConnection }

import org.slf4j.LoggerFactory

class EnvironmentConnection(url: URL) extends URLConnection(url) {

  val logger = LoggerFactory.getLogger(getClass)

  def connect(): Unit = {}

  override def getInputStream: InputStream = mkInputStream(url)

  private def mkInputStream(url: URL): InputStream = {
    val path = url.getPath
    val env  = resolveEnvironment
    logger.info(s"env(path: $path) - $env")
    getClass.getResourceAsStream(path.replace("<ENVIRONMENT>", env))
  }

  private def resolveEnvironment: String = {
    val property = Option(System.getProperty("environment"))
    val system   = Option(System.getenv("SYSTEM_ENV"))
    (property, system) match {
      case (Some(env), _) => env // -Denvironment=[production|staging|development]
      case (_, Some(env)) => env // export SYSTEM_ENV=[production|staging|development]
      case _              => "development" // default environment
    }
  }

}
