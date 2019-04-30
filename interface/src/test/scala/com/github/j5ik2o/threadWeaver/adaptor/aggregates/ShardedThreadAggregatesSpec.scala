package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.ActorSystem
import akka.cluster.MemberStatus
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{ Cluster, Join }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.{ CreateThread, CreateThreadResponse }
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ AdministratorIds, MemberIds, ThreadId }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.typesafe.config.ConfigFactory
import org.scalatest.FreeSpecLike

import scala.concurrent.duration._

class ShardedThreadAggregatesSpec
    extends ScalaTestWithActorTestKit(
      ConfigFactory.parseString("""
      |akka.actor.provider = cluster
      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      |passivate-timeout = 60 seconds
    """.stripMargin).withFallback(ConfigFactory.load())
    )
    with FreeSpecLike
    with ActorSpecSupport {

  val cluster: Cluster                 = Cluster(system)
  val clusterSharding: ClusterSharding = ClusterSharding(typedSystem)

  def typedSystem[T]: ActorSystem[T] = system.asInstanceOf[ActorSystem[T]]

  "ShardedThreadAggregates" - {
    "sharding" in {
      cluster.manager ! Join(cluster.selfMember.address)
      eventually {
        cluster.selfMember.status shouldEqual MemberStatus.Up
      }
      ShardedThreadAggregates.initEntityActor(clusterSharding, 1 hours)

      val probe           = TestProbe[CreateThreadResponse]()(typedSystem)
      val threadId        = ThreadId()
      val administratorId = AccountId()
      val threadRef       = clusterSharding.entityRefFor(ShardedThreadAggregates.TypeKey, threadId.value.toString)
      threadRef ! CreateThread(
        ULID(),
        threadId,
        None,
        AdministratorIds(administratorId),
        MemberIds.empty,
        Instant.now,
        Some(probe.ref)
      )
      val result = probe.expectMessageType[CreateThreadResponse]
      result.threadId shouldBe threadId
    }
  }
}
