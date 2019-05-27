package com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.ActorRef
import ThreadProtocol.{ DestroyThreadResponse, _ }
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import org.scalatest.{ FreeSpecLike, Matchers }

class ThreadAggregateSpec extends ScalaTestWithActorTestKit with FreeSpecLike with Matchers {

  def newThreadRef(threadId: ThreadId): ActorRef[CommandRequest] = spawn(ThreadAggregate.behavior(threadId, Seq.empty))

  "ThreadAggregate" - {
    "create" in {
      val threadId          = ThreadId()
      val threadRef         = newThreadRef(threadId)
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

      val createThreadSucceeded = createThreadProbe.expectMessageType[CreateThreadSucceeded]
      createThreadSucceeded.threadId shouldBe threadId
      createThreadSucceeded.createAt shouldBe now

      val existsThreadProbe = TestProbe[ExistsThreadResponse]()
      threadRef ! ExistsThread(ULID(), threadId, administratorId, now, existsThreadProbe.ref)
      val existsThreadSucceeded = existsThreadProbe.expectMessageType[ExistsThreadSucceeded]
      existsThreadSucceeded.exists shouldBe true
    }
    "destroy" in {
      val threadId          = ThreadId()
      val threadRef         = newThreadRef(threadId)
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

      val createThreadResponse = createThreadProbe.expectMessageType[CreateThreadResponse]
      createThreadResponse match {
        case f: CreateThreadFailed =>
          fail(f.message)
        case s: CreateThreadSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      val destroyThreadProbe = TestProbe[DestroyThreadResponse]()
      threadRef ! DestroyThread(ULID(), threadId, administratorId, now, Some(destroyThreadProbe.ref))

      val destroyThreadResponse = destroyThreadProbe.expectMessageType[DestroyThreadResponse]
      destroyThreadResponse match {
        case f: DestroyThreadFailed =>
          fail(f.message)
        case s: DestroyThreadSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      val existsThreadProbe = TestProbe[ExistsThreadResponse]()
      threadRef ! ExistsThread(ULID(), threadId, administratorId, now, existsThreadProbe.ref)
      val existsThreadSucceeded = existsThreadProbe.expectMessageType[ExistsThreadSucceeded]
      existsThreadSucceeded.exists shouldBe false
    }
    "join administrator" in {
      val threadId          = ThreadId()
      val threadRef         = newThreadRef(threadId)
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

      val administratorId2                  = AccountId()
      val joinAdministratorIdsResponseProbe = TestProbe[JoinAdministratorIdsResponse]()

      threadRef ! JoinAdministratorIds(
        ULID(),
        threadId,
        administratorId,
        AdministratorIds(administratorId2),
        now,
        Some(joinAdministratorIdsResponseProbe.ref)
      )

      joinAdministratorIdsResponseProbe.expectMessageType[JoinAdministratorIdsResponse] match {
        case f: JoinAdministratorIdsFailed =>
          fail(f.message)
        case s: JoinAdministratorIdsSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      val getAdministratorIdsResponseProbe = TestProbe[GetAdministratorIdsResponse]()

      threadRef ! GetAdministratorIds(ULID(), threadId, administratorId, now, getAdministratorIdsResponseProbe.ref)

      getAdministratorIdsResponseProbe.expectMessageType[GetAdministratorIdsResponse] match {
        case f: GetAdministratorIdsFailed =>
          fail(f.message)
        case s: GetAdministratorIdsSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

    }
    "join members" in {
      val threadId          = ThreadId()
      val threadRef         = newThreadRef(threadId)
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

      val memberId                   = AccountId()
      val joinMemberIdsResponseProbe = TestProbe[JoinMemberIdsResponse]()

      threadRef ! JoinMemberIds(
        ULID(),
        threadId,
        administratorId,
        MemberIds(memberId),
        now,
        Some(joinMemberIdsResponseProbe.ref)
      )

      joinMemberIdsResponseProbe.expectMessageType[JoinMemberIdsResponse] match {
        case f: JoinMemberIdsFailed =>
          fail(f.message)
        case s: JoinMemberIdsSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      val getMemberIdsResponseProbe = TestProbe[GetMemberIdsResponse]()

      threadRef ! GetMemberIds(ULID(), threadId, administratorId, now, getMemberIdsResponseProbe.ref)

      getMemberIdsResponseProbe.expectMessageType[GetMemberIdsResponse] match {
        case f: GetMemberIdsFailed =>
          fail(f.message)
        case s: GetMemberIdsSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }
    }
    "add messages" in {
      val threadId          = ThreadId()
      val threadRef         = newThreadRef(threadId)
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

      val createThreadSucceeded = createThreadProbe.expectMessageType[CreateThreadSucceeded]
      createThreadSucceeded.threadId shouldBe threadId
      createThreadSucceeded.createAt shouldBe now

      val memberId                   = AccountId()
      val joinMemberIdsResponseProbe = TestProbe[JoinMemberIdsResponse]()

      threadRef ! JoinMemberIds(
        ULID(),
        threadId,
        administratorId,
        MemberIds(memberId),
        now,
        Some(joinMemberIdsResponseProbe.ref)
      )

      joinMemberIdsResponseProbe.expectMessageType[JoinMemberIdsResponse] match {
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
    }
  }
}
