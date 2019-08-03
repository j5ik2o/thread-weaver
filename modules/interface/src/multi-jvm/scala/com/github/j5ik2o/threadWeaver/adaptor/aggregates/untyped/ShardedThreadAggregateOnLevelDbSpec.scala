package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped
import java.time.Instant

import akka.actor.{ ActorIdentity, Identify, Props }
import akka.persistence.Persistence
import akka.persistence.journal.leveldb.{ SharedLeveldbJournal, SharedLeveldbStore }
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.DynamoDbConfig.{ controller, node1, node2 }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.{
  CreateThread,
  CreateThreadFailed,
  CreateThreadResponse,
  CreateThreadSucceeded
}
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.{ LevelDbConfig, LevelDbSpecSupport }
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ AdministratorIds, MemberIds, ThreadId, ThreadTitle }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

class ShardedThreadAggregateOnLevelDbMultiJvmNode1 extends ShardedThreadAggregateOnLevelDbSpec
class ShardedThreadAggregateOnLevelDbMultiJvmNode2 extends ShardedThreadAggregateOnLevelDbSpec
class ShardedThreadAggregateOnLevelDbMultiJvmNode3 extends ShardedThreadAggregateOnLevelDbSpec
import scala.concurrent.duration._

class ShardedThreadAggregateOnLevelDbSpec
    extends MultiNodeSpec(LevelDbConfig)
    with LevelDbSpecSupport
    with ImplicitSender {
  override def initialParticipants: Int = roles.size

  "ShardedThreadAggregate" - {
    "setup shared journal" in {
      Persistence(system)
      runOn(controller) {
        system.actorOf(Props[SharedLeveldbStore], "store")
      }
      enterBarrier("persistence start")
      runOn(node1, node2) {
        system.actorSelection(node(controller) / "user" / "store") ! Identify(None)
        val sharedStore = expectMsgType[ActorIdentity].ref.get
        SharedLeveldbJournal.setStore(sharedStore, system)
      }
      enterBarrier("persistence started")
    }
    "join cluster" in within(15 seconds) {
      enterBarrier("join node1")
      join(node1, node1) {
        ShardedThreadAggregatesRegion.startClusterSharding(Seq.empty)
      }
      enterBarrier("join node2")
      join(node2, node1) {
        ShardedThreadAggregatesRegion.startClusterSharding(Seq.empty)
      }
      enterBarrier("join all nodes to the cluster")
    }
    "createThread" in {
      runOn(node1) {
        val accountId = AccountId()
        val threadId  = ThreadId()
        val title     = ThreadTitle("test")
        val threadRef = ShardedThreadAggregatesRegion.shardRegion
        threadRef ! CreateThread(
          ULID(),
          threadId,
          accountId,
          None,
          title,
          None,
          AdministratorIds(accountId),
          MemberIds.empty,
          Instant.now,
          reply = true
        )
        expectMsgType[CreateThreadResponse](10 seconds) match {
          case f: CreateThreadFailed =>
            fail(f.message)
          case s: CreateThreadSucceeded =>
            s.threadId shouldBe threadId
        }
      }
      enterBarrier("created thread")
    }
  }
}
