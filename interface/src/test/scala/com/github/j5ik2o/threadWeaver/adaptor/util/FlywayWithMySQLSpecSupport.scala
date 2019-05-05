package com.github.j5ik2o.threadWeaver.adaptor.util

import java.io.File

import com.github.j5ik2o.scalatestplus.db._
import com.wix.mysql.distribution.Version._
import org.scalatest.TestSuite
import org.seasar.util.io.ResourceUtil

import scala.concurrent.duration._

trait FlywayWithMySQLSpecSupport extends FlywayWithMySQLdOneInstancePerSuite {
  this: TestSuite =>

  override protected lazy val mySQLdConfig: MySQLdConfig = MySQLdConfig(
    version = v5_6_21,
    port = Some(RandomPortSupport.temporaryServerPort()),
    userWithPassword = Some(UserWithPassword("tw", "passwd")),
    timeout = Some((30 seconds) * sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1").toDouble)
  )

  override protected lazy val downloadConfig: DownloadConfig =
    super.downloadConfig.copy(cacheDir = new File(sys.env("HOME") + "/.wixMySQL/downloads"))

  override protected lazy val schemaConfigs: Seq[SchemaConfig] = Seq(SchemaConfig(name = "tw"))

  override protected def flywayConfig(jdbcUrl: String): FlywayConfig = {
    val buildDir = ResourceUtil.getBuildDir(getClass)
    val file     = new File(buildDir, "/../../../../tools/flyway/src/test/resources/db-migration")
    FlywayConfig(
      locations = Seq(
        s"filesystem:${file.getAbsolutePath}"
      ),
      placeholderConfig = Some(
        PlaceholderConfig(
          placeholderReplacement = true,
          placeholders = Map("engineName" -> "MEMORY", "idSequenceNumberEngineName" -> "MyISAM")
        )
      )
    )
  }

}
