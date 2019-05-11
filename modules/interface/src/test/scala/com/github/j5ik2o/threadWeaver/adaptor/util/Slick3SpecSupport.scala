package com.github.j5ik2o.threadWeaver.adaptor.util

import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfter, BeforeAndAfterAll, Suite }
import slick.basic.DatabaseConfig
import slick.jdbc.SetParameter.SetUnit
import slick.jdbc.{ JdbcProfile, SQLActionBuilder }

trait Slick3SpecSupport extends BeforeAndAfter with BeforeAndAfterAll with ScalaFutures with JdbcSpecSupport {
  self: Suite with FlywayWithMySQLSpecSupport =>

  private var _dbConfig: DatabaseConfig[JdbcProfile] = _

  private var _profile: JdbcProfile = _

  protected def dbConfig: DatabaseConfig[JdbcProfile] = _dbConfig

  // protected def profile: JdbcProfile = _profile

  after {
    val profile = dbConfig.profile
    import profile.api._
    implicit val ec = dbConfig.db.executor.executionContext
    val actions = tables.map { table =>
      SQLActionBuilder(List(s"TRUNCATE TABLE $table"), SetUnit).asUpdate
    }
    dbConfig.db.run(DBIO.sequence(actions).transactionally)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val config = ConfigFactory.parseString(s"""
                                              |slick {
                                              |  profile = "slick.jdbc.MySQLProfile$$"
                                              |  db {
                                              |    connectionPool = disabled
                                              |    driver = "com.mysql.jdbc.Driver"
                                              |    url = "jdbc:mysql://localhost:$jdbcPort/tw?useSSL=false"
                                              |    user = "tw"
                                              |    password = "passwd"
                                              |  }
                                              |}
      """.stripMargin)
    _dbConfig = DatabaseConfig.forConfig[JdbcProfile]("slick", config)
    _profile = dbConfig.profile
  }

  override protected def afterAll(): Unit = {
    dbConfig.db.shutdown
    super.afterAll()
  }

}
