package com.github.j5ik2o.threadWeaver.api.config

import java.net.{ URL, URLConnection, URLStreamHandler }

class EnvironmentURLStreamHandler extends URLStreamHandler {

  def openConnection(url: URL): URLConnection = {
    new EnvironmentConnection(url)
  }

}
