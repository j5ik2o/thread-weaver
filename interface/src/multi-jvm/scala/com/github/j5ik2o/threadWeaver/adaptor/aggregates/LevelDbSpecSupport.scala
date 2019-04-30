package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.io.File

import akka.cluster.Cluster
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{ MultiNodeSpec, MultiNodeSpecCallbacks }
import org.apache.commons.io.FileUtils
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }

trait LevelDbSpecSupport extends MultiNodeSpecCallbacks with FreeSpecLike with Matchers with BeforeAndAfterAll {
  this: MultiNodeSpec =>

  import MultiNodeLevelDbConfig._

  val storageLocations: List[File] =
    List("akka.persistence.journal.leveldb.dir",
         "akka.persistence.journal.leveldb-shared.store.dir",
         "akka.persistence.snapshot-store.local.dir").map(s => new File(system.settings.config.getString(s)))

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  def join(from: RoleName, to: RoleName)(f: => Unit): Unit = {
    runOn(from) {
      Cluster(system) join node(to).address
      f
    }
    enterBarrier(from.name + "-joined")
  }

  override protected def atStartup() {
    runOn(controller) {
      storageLocations.foreach(dir => FileUtils.deleteDirectory(dir))
    }
  }

  override protected def afterTermination() {
    runOn(controller) {
      storageLocations.foreach(dir => FileUtils.deleteDirectory(dir))
    }
  }

}
