package com.github.j5ik2o.threadWeave.adaptor.aggregates

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import com.github.j5ik2o.threadWeave.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeave.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeave.domain.model.threads._
import com.github.j5ik2o.threadWeave.infrastructure.ulid.ULID
import org.scalatest.FreeSpecLike

class PersistentThreadAggregateSpec
    extends ScalaTestWithActorTestKit
    with FreeSpecLike
    with ActorSpecSupport
    with PersistenceCleanup {

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
      val threadRef         = spawn(PersistentThreadAggregate.behavior(threadId))
      val now               = Instant.now
      val createThreadProbe = TestProbe[CreateThreadResponse]()
      val administratorId   = AccountId()

      threadRef ! CreateThread(
        ULID(),
        threadId,
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

      val memberId             = AccountId()
      val addMemberIdsResponse = TestProbe[AddMemberIdsResponse]()

      threadRef ! AddMemberIds(
        ULID(),
        threadId,
        MemberIds(memberId),
        administratorId,
        now,
        Some(addMemberIdsResponse.ref)
      )

      addMemberIdsResponse.expectMessageType[AddMemberIdsResponse] match {
        case f: AddMemberIdsFailed =>
          fail(f.message)
        case s: AddMemberIdsSucceeded =>
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

      val threadRef2 = spawn(PersistentThreadAggregate.behavior(threadId))

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
