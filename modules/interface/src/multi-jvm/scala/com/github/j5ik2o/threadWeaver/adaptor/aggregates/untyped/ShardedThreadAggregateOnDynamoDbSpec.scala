package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import java.time.Instant

import akka.persistence.Persistence
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.DynamoDbConfig.{ controller, node1, node2 }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.{
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

  override def initialParticipants: Int = roles.size

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
        ShardedThreadAggregatesRegion.startClusterSharding(Seq.empty)
      }
      enterBarrier("after-1")
      join(node1, controller) {
        ShardedThreadAggregatesRegion.startClusterSharding(Seq.empty)
      }
      enterBarrier("after-2")
      join(node2, controller) {
        ShardedThreadAggregatesRegion.startClusterSharding(Seq.empty)
      }
      enterBarrier("after-3")
    }
    "createThread" in {
      runOn(node1) {
        val accountId = AccountId()
        val threadId  = ThreadId()
        val title     = ThreadTitle("test")
        val threadRef = ShardedThreadAggregatesRegion.shardRegion
        threadRef ! CreateThread(ULID(),
                                 threadId,
                                 accountId,
                                 None,
                                 title,
                                 None,
                                 AdministratorIds(accountId),
                                 MemberIds.empty,
                                 Instant.now,
                                 true)
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
