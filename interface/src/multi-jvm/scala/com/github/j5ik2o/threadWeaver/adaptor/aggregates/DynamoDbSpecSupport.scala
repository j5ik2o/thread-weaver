package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import akka.cluster.Cluster
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{ MultiNodeSpec, MultiNodeSpecCallbacks }
import com.github.j5ik2o.threadWeaver.adaptor.util.DynamoDBSpecSupport
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }

trait DynamoDbSpecSupport
    extends MultiNodeSpecCallbacks
    with FreeSpecLike
    with Matchers
    with BeforeAndAfterAll
    with DynamoDBSpecSupport {
  this: MultiNodeSpec =>

  import DynamoDbConfig._

  override protected lazy val dynamoDBPort: Int = 8000

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  def join(from: RoleName, to: RoleName)(f: => Unit): Unit = {
    runOn(from) {
      Cluster(system) join node(to).address
      f
    }
    enterBarrier(from.name + "-joined")
  }

  override protected def atStartup() {}

  override protected def afterTermination() {
    runOn(controller) {
      shutdownDynamoDBLocal()
    }
  }

}
