package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import java.time.Instant

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.adaptor.util.{ DynamoDBSpecSupport, RandomPortSupport }
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.typesafe.config.ConfigFactory

object PersistentThreadAggregateOnDynamoDBSpec extends RandomPortSupport {
  lazy val dbPort: Int = temporaryServerPort()
}

class PersistentThreadAggregateOnDynamoDBSpec
    extends AkkaSpec(
      ConfigFactory.parseString(s"""
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
      """.stripMargin).withFallback(ConfigFactory.load())
    )
    with DynamoDBSpecSupport {

  override protected lazy val dynamoDBPort: Int = PersistentThreadAggregateOnDynamoDBSpec.dbPort

  override protected def atStartup(): Unit = {
    startDynamoDBLocal()
    createJournalTable()
    createSnapshotTable()
  }

  override protected def beforeTermination(): Unit = {
    shutdownDynamoDBLocal()
  }

  "PersistentThreadAggregate" - {
    "add messages" in {
      val threadId        = ThreadId()
      val threadRef       = system.actorOf(PersistentThreadAggregate.props(threadId)(Seq.empty))
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
        true
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
        true
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
        true
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

      val threadRef2 = system.actorOf(PersistentThreadAggregate.props(threadId)(Seq.empty))

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
