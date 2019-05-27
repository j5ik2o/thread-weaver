package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import akka.actor._
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.Persistence
import akka.persistence.journal.leveldb.{ SharedLeveldbJournal, SharedLeveldbStore }
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ThreadProtocol.{
  CreateThread,
  CreateThreadFailed,
  CreateThreadResponse,
  CreateThreadSucceeded
}
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ShardedThreadAggregates
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ AdministratorIds, MemberIds, ThreadId, ThreadTitle }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

import scala.concurrent.duration._

class ShardedThreadAggregateOnLevelDbMultiJvmNode1 extends ShardedThreadAggregateOnLevelDbSpec
class ShardedThreadAggregateOnLevelDbMultiJvmNode2 extends ShardedThreadAggregateOnLevelDbSpec
class ShardedThreadAggregateOnLevelDbMultiJvmNode3 extends ShardedThreadAggregateOnLevelDbSpec

class ShardedThreadAggregateOnLevelDbSpec
    extends MultiNodeSpec(LevelDbConfig)
    with LevelDbSpecSupport
    with ImplicitSender {
  import DynamoDbConfig._

  override def initialParticipants: Int = roles.size

  implicit def typedSystem[T]: ActorSystem[T] = system.toTyped.asInstanceOf[ActorSystem[T]]

  "ShardedThreadAggregate" - {
    "setup shared journal" in {
      Persistence(system)
      runOn(controller) {
        system.actorOf(Props[SharedLeveldbStore], "store")
      }
      enterBarrier("persistence-started")
      runOn(node1, node2) {
        system.actorSelection(node(controller) / "user" / "store") ! Identify(None)
        val sharedStore = expectMsgType[ActorIdentity].ref.get
        SharedLeveldbJournal.setStore(sharedStore, system)
      }
      enterBarrier("after-1")
    }
    "join cluster" in within(15 seconds) {
      join(node1, node1) {
        ShardedThreadAggregates.initEntityActor(ClusterSharding(typedSystem), 1 hours, Seq.empty)
      }
      join(node2, node1) {
        ShardedThreadAggregates.initEntityActor(ClusterSharding(typedSystem), 1 hours, Seq.empty)
      }
      enterBarrier("after-2")
    }
    "createThread" in {
      runOn(node1) {
        val accountId = AccountId()
        val threadId  = ThreadId()
        val title     = ThreadTitle("test")
        val threadRef =
          ClusterSharding(typedSystem).entityRefFor(ShardedThreadAggregates.TypeKey, threadId.value.asString)
        val createThreadResponseProbe = TestProbe[CreateThreadResponse]
        threadRef ! CreateThread(ULID(),
                                 threadId,
                                 accountId,
                                 None,
                                 title,
                                 None,
                                 AdministratorIds(accountId),
                                 MemberIds.empty,
                                 Instant.now,
                                 Some(createThreadResponseProbe.ref))
        createThreadResponseProbe.expectMessageType[CreateThreadResponse](10 seconds) match {
          case f: CreateThreadFailed =>
            fail(f.message)
          case s: CreateThreadSucceeded =>
            s.threadId shouldBe threadId
        }
      }
      enterBarrier("after-3")
    }
  }
}
