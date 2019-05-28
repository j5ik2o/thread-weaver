package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import java.time.Instant

import akka.actor.ActorRef
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

class ThreadAggregateSpec extends AkkaSpec {
  def newThreadRef(threadId: ThreadId): ActorRef = system.actorOf(ThreadAggregate.props(threadId, Seq.empty))

  "ThreadAggregate" - {
    "create" in {
      val threadId        = ThreadId()
      val threadRef       = newThreadRef(threadId)
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

      val createThreadSucceeded = expectMsgType[CreateThreadSucceeded]
      createThreadSucceeded.threadId shouldBe threadId
      createThreadSucceeded.createAt shouldBe now

      threadRef ! ExistsThread(ULID(), threadId, administratorId, now)
      val existsThreadSucceeded = expectMsgType[ExistsThreadSucceeded]
      existsThreadSucceeded.exists shouldBe true
    }
    "destroy" in {
      val threadId        = ThreadId()
      val threadRef       = newThreadRef(threadId)
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

      val createThreadResponse = expectMsgType[CreateThreadResponse]
      createThreadResponse match {
        case f: CreateThreadFailed =>
          fail(f.message)
        case s: CreateThreadSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      threadRef ! DestroyThread(ULID(), threadId, administratorId, now, reply = true)

      val destroyThreadResponse = expectMsgType[DestroyThreadResponse]
      destroyThreadResponse match {
        case f: DestroyThreadFailed =>
          fail(f.message)
        case s: DestroyThreadSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      threadRef ! ExistsThread(ULID(), threadId, administratorId, now)
      val existsThreadSucceeded = expectMsgType[ExistsThreadSucceeded]
      existsThreadSucceeded.exists shouldBe false
    }
    "join administrator" in {
      val threadId        = ThreadId()
      val threadRef       = newThreadRef(threadId)
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

      val administratorId2 = AccountId()

      threadRef ! JoinAdministratorIds(
        ULID(),
        threadId,
        administratorId,
        AdministratorIds(administratorId2),
        now,
        reply = true
      )

      expectMsgType[JoinAdministratorIdsResponse] match {
        case f: JoinAdministratorIdsFailed =>
          fail(f.message)
        case s: JoinAdministratorIdsSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

      threadRef ! GetAdministratorIds(ULID(), threadId, administratorId, now)

      expectMsgType[GetAdministratorIdsResponse] match {
        case f: GetAdministratorIdsFailed =>
          fail(f.message)
        case s: GetAdministratorIdsSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }

    }
    "join members" in {
      val threadId        = ThreadId()
      val threadRef       = newThreadRef(threadId)
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

      threadRef ! GetMemberIds(ULID(), threadId, administratorId, now)

      expectMsgType[GetMemberIdsResponse] match {
        case f: GetMemberIdsFailed =>
          fail(f.message)
        case s: GetMemberIdsSucceeded =>
          s.threadId shouldBe threadId
          s.createAt shouldBe now
      }
    }
    "add messages" in {
      val threadId        = ThreadId()
      val threadRef       = newThreadRef(threadId)
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

      val createThreadSucceeded = expectMsgType[CreateThreadSucceeded]
      createThreadSucceeded.threadId shouldBe threadId
      createThreadSucceeded.createAt shouldBe now

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
    }
  }
}
