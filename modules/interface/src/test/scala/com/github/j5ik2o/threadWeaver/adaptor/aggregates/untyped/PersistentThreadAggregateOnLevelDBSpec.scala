package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.PersistenceCleanup
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.typesafe.config.ConfigFactory

class PersistentThreadAggregateOnLevelDBSpec
    extends AkkaSpec(
      ConfigFactory
        .parseString("""
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
        """.stripMargin).withFallback(
          ConfigFactory.load()
        )
    )
    with PersistenceCleanup {
  override protected def atStartup(): Unit = deleteStorageLocations(system)

  override protected def beforeTermination(): Unit = deleteStorageLocations(system)

  "PersistentThreadAggregate" - {
    "add messages" in {
      val threadId        = ThreadId()
      val threadRef       = system.actorOf(PersistentThreadAggregate.props(threadId, Seq.empty))
      val now             = Instant.now
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
        now,
        reply = true
      )

      expectMsgType[CreateThreadResponse] match {
        case f: CreateThreadFailed =>
          fail(f.message)
        case s: CreateThreadSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      val memberId = AccountId()

      threadRef ! JoinMemberIds(
        ULID(),
        threadId,
        administratorId,
        MemberIds(memberId),
        now,
        reply = true
      )

      expectMsgType[JoinMemberIdsResponse] match {
        case f: JoinMemberIdsFailed =>
          fail(f.message)
        case s: JoinMemberIdsSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      val messages = Messages(TextMessage(MessageId(), None, ToAccountIds.empty, Text("ABC"), memberId, now, now))
      threadRef ! AddMessages(
        ULID(),
        threadId,
        messages,
        now,
        reply = true
      )

      expectMsgType[AddMessagesResponse] match {
        case f: AddMessagesFailed =>
          fail(f.message)
        case s: AddMessagesSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      threadRef ! GetMessages(ULID(), threadId, memberId, now)
      expectMsgType[GetMessagesResponse] match {
        case f: GetMessagesFailed =>
          fail(f.message)
        case s: GetMessagesSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
          s.messages shouldBe messages
      }

      // アクターを停止する
      killActors(threadRef)

      val threadRef2 = system.actorOf(PersistentThreadAggregate.props(threadId, Seq.empty))

      threadRef2 ! GetMessages(ULID(), threadId, memberId, now)
      expectMsgType[GetMessagesResponse] match {
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
