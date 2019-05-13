package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.ActorSystem
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.typesafe.config.ConfigFactory
import org.scalatest.FreeSpecLike

class PersistentThreadAggregateOnLevelDBSpec
    extends ScalaTestWithActorTestKit(ConfigFactory.parseString("""
        |akka {
        |  persistence {
        |    journal {
        |      plugin = akka.persistence.journal.leveldb
        |      leveldb {
        |        dir = "target/persistence/journal"
        |        native = on
        |      }
        |    }
        |    snapshot-store {
        |      plugin = akka.persistence.snapshot-store.local
        |      local.dir = "target/persistence/snapshots"
        |    }
        |  }
        |}
      """.stripMargin).withFallback(ConfigFactory.load()))
    with FreeSpecLike
    with ActorSpecSupport
    with PersistenceCleanup {

  override def typedSystem: ActorSystem[Nothing] = system

  override protected def beforeAll(): Unit = {
    deleteStorageLocations()
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    deleteStorageLocations()
    super.afterAll()
  }

  "PersistentThreadAggregate" - {
    "add messages" in {
      val threadId          = ThreadId()
      val threadRef         = spawn(PersistentThreadAggregate.behavior(threadId, Seq.empty))
      val now               = Instant.now
      val createThreadProbe = TestProbe[CreateThreadResponse]()
      val administratorId   = AccountId()
      val title             = ThreadTitle("test")
      threadRef ! CreateThread(
        ULID(),
        threadId,
        administratorId,
        None,
        title,
        None,
        AdministratorIds(administratorId),
        MemberIds.empty,
        now,
        Some(createThreadProbe.ref)
      )

      createThreadProbe.expectMessageType[CreateThreadResponse] match {
        case f: CreateThreadFailed =>
          fail(f.message)
        case s: CreateThreadSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      val memberId              = AccountId()
      val joinMemberIdsResponse = TestProbe[JoinMemberIdsResponse]()

      threadRef ! JoinMemberIds(
        ULID(),
        threadId,
        administratorId,
        MemberIds(memberId),
        now,
        Some(joinMemberIdsResponse.ref)
      )

      joinMemberIdsResponse.expectMessageType[JoinMemberIdsResponse] match {
        case f: JoinMemberIdsFailed =>
          fail(f.message)
        case s: JoinMemberIdsSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      val addMessagesResponseProbe = TestProbe[AddMessagesResponse]()
      val messages                 = Messages(TextMessage(MessageId(), None, ToAccountIds.empty, Text("ABC"), memberId, now, now))
      threadRef ! AddMessages(
        ULID(),
        threadId,
        messages,
        now,
        Some(addMessagesResponseProbe.ref)
      )

      addMessagesResponseProbe.expectMessageType[AddMessagesResponse] match {
        case f: AddMessagesFailed =>
          fail(f.message)
        case s: AddMessagesSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      val getMessagesResponseProbe = TestProbe[GetMessagesResponse]()
      threadRef ! GetMessages(ULID(), threadId, memberId, now, getMessagesResponseProbe.ref)
      getMessagesResponseProbe.expectMessageType[GetMessagesResponse] match {
        case f: GetMessagesFailed =>
          fail(f.message)
        case s: GetMessagesSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
          s.messages shouldBe messages
      }

      // アクターを停止する
      killActors(threadRef)

      val threadRef2 = spawn(PersistentThreadAggregate.behavior(threadId, Seq.empty))

      val getMessagesResponseProbe2 = TestProbe[GetMessagesResponse]()
      threadRef2 ! GetMessages(ULID(), threadId, memberId, now, getMessagesResponseProbe2.ref)
      getMessagesResponseProbe2.expectMessageType[GetMessagesResponse] match {
        case f: GetMessagesFailed =>
          fail(f.message)
        case s: GetMessagesSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
          s.messages shouldBe messages
      }
    }
  }

}
