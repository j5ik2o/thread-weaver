package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import java.time.Instant

import akka.cluster.{ Cluster, MemberStatus }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.{ CreateThread, CreateThreadResponse }
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ AdministratorIds, MemberIds, ThreadId, ThreadTitle }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.typesafe.config.ConfigFactory

class ShardedThreadAggregatesSpec
    extends AkkaSpec(
      ConfigFactory
        .parseString("""
          |akka.actor.provider = cluster
          |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
          |passivate-timeout = 60 seconds
        """.stripMargin).withFallback(
          ConfigFactory.load()
        )
    ) {
  val cluster = Cluster(system)
  "ShardedThreadAggregates" - {
    "sharding" in {
      cluster.join(cluster.selfAddress)
      awaitAssert {
        cluster.selfMember.status shouldEqual MemberStatus.Up
      }
      ShardedThreadAggregatesRegion.startClusterSharding(10, Seq.empty)
      val threadRef = ShardedThreadAggregatesRegion.shardRegion

      val threadId        = ThreadId()
      val administratorId = AccountId()
      val title           = ThreadTitle("test")
      threadRef ! CreateThread(
        ULID(),
        threadId,
        administratorId,
        None,
        title,
        None,
        AdministratorIds(administratorId),
        MemberIds.empty,
        Instant.now,
        reply = true
      )
      val result = expectMsgType[CreateThreadResponse]
      result.threadId shouldBe threadId
    }
  }
}
