package com.github.j5ik2o.threadWeaver.api.config

import java.net.{ URLStreamHandler, URLStreamHandlerFactory }

class EnvironmentURLStreamHandlerFactory extends URLStreamHandlerFactory {
  override def createURLStreamHandler(protocol: String): URLStreamHandler = {
    protocol match {
      case p if p.startsWith("environment") => new EnvironmentURLStreamHandler
      case _                                => null
    }
  }
}
