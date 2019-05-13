package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import com.github.j5ik2o.reactive.aws.test.RandomPortSupport
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.adaptor.util.{ DynamoDBSpecSupport, ScalaFuturesSpecSupport }
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.typesafe.config.ConfigFactory
import org.scalatest.FreeSpecLike

object PersistentThreadAggregateOnDynamoDBSpec extends RandomPortSupport {
  lazy val dbPort: Int = temporaryServerPort()
}

class PersistentThreadAggregateOnDynamoDBSpec
    extends ScalaTestWithActorTestKit(ConfigFactory.parseString(s"""
        |akka {
        |  persistence {
        |    journal {
        |      plugin = dynamo-db-journal
        |    }
        |    snapshot-store {
        |      plugin = dynamo-db-snapshot
        |    }
        |  }
        |}
        |
        |dynamo-db-journal {
        |  dynamodb-client {
        |    access-key-id = "x"
        |    secret-access-key = "x"
        |    endpoint = "http://127.0.0.1:${PersistentThreadAggregateOnDynamoDBSpec.dbPort}/"
        |  }
        |}
        |
        |dynamo-db-snapshot {
        |  dynamodb-client {
        |    access-key-id = "x"
        |    secret-access-key = "x"
        |    endpoint = "http://127.0.0.1:${PersistentThreadAggregateOnDynamoDBSpec.dbPort}/"
        |  }
        |}
      """.stripMargin).withFallback(ConfigFactory.load()))
    with FreeSpecLike
    with ActorSpecSupport
    with DynamoDBSpecSupport
    with ScalaFuturesSpecSupport {

  override protected lazy val dynamoDBPort: Int = PersistentThreadAggregateOnDynamoDBSpec.dbPort

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createJournalTable()
    createSnapshotTable()
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
