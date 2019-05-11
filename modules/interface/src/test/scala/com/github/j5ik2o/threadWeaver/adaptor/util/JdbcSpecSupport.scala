package com.github.j5ik2o.threadWeaver.adaptor.util

import org.scalatest.concurrent.ScalaFutures

trait JdbcSpecSupport extends ScalaFutures {
  this: FlywayWithMySQLSpecSupport =>
  val tables: Seq[String]

  def jdbcPort: Int = mySQLdConfig.port.get

}
