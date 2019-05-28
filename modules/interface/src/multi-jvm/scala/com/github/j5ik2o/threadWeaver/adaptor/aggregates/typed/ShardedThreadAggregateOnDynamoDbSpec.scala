package com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed

import java.time.Instant

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.Persistence
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ThreadProtocol.{
  CreateThread,
  CreateThreadFailed,
  CreateThreadResponse,
  CreateThreadSucceeded
}
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.{ DynamoDbConfig, DynamoDbSpecSupport }
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ AdministratorIds, MemberIds, ThreadId, ThreadTitle }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

import scala.concurrent.duration._

class ShardedThreadAggregateOnDynamoDbMultiJvmNode1 extends ShardedThreadAggregateOnDynamoDbSpec
class ShardedThreadAggregateOnDynamoDbMultiJvmNode2 extends ShardedThreadAggregateOnDynamoDbSpec
class ShardedThreadAggregateOnDynamoDbMultiJvmNode3 extends ShardedThreadAggregateOnDynamoDbSpec

class ShardedThreadAggregateOnDynamoDbSpec
    extends MultiNodeSpec(DynamoDbConfig)
    with DynamoDbSpecSupport
    with ImplicitSender {
  import DynamoDbConfig._

  override def initialParticipants: Int = roles.size

  implicit def typedSystem[T]: ActorSystem[T] = system.toTyped.asInstanceOf[ActorSystem[T]]

  var clusterSharding1: ClusterSharding = _

  "ShardedThreadAggregate" - {
    "setup shared journal" in {
      Persistence(system)
      runOn(controller) {
        startDynamoDBLocal()
      }
      enterBarrier("start dynamo-db")
      runOn(controller, node1, node2) {
        waitDynamoDBLocal()
      }
      enterBarrier("wait dynamo-db")
      runOn(controller) {
        createJournalTable()
        createSnapshotTable()
      }
      enterBarrier("wait creating table")
    }
    "join cluster" in within(15 seconds) {
      join(controller, controller) {
        val clusterSharding = ClusterSharding(typedSystem)
        ShardedThreadAggregates.initEntityActor(clusterSharding, 1 hours, Seq.empty)
      }
      enterBarrier("after-1")
      join(node1, controller) {
        clusterSharding1 = ClusterSharding(typedSystem)
        ShardedThreadAggregates.initEntityActor(clusterSharding1, 1 hours, Seq.empty)
      }
      enterBarrier("after-2")
      join(node2, controller) {
        val clusterSharding = ClusterSharding(typedSystem)
        ShardedThreadAggregates.initEntityActor(clusterSharding, 1 hours, Seq.empty)
      }
      enterBarrier("after-3")
    }
    "createThread" in {
      runOn(node1) {
        val accountId = AccountId()
        val threadId  = ThreadId()
        val title     = ThreadTitle("test")
        val threadRef =
          clusterSharding1.entityRefFor(ShardedThreadAggregates.TypeKey, threadId.value.asString)
        threadRef ! CreateThread(ULID(),
                                 threadId,
                                 accountId,
                                 None,
                                 title,
                                 None,
                                 AdministratorIds(accountId),
                                 MemberIds.empty,
                                 Instant.now,
                                 Some(self.toTyped[CreateThreadResponse]))
        expectMsgType[CreateThreadResponse](10 seconds) match {
          case f: CreateThreadFailed =>
            fail(f.message)
          case s: CreateThreadSucceeded =>
            s.threadId shouldBe threadId
        }
      }
      enterBarrier("after-4")
    }
  }
}
