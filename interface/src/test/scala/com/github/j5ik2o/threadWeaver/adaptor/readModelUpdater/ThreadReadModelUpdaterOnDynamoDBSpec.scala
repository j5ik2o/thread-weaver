package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.scaladsl.adapter._
import akka.persistence.query.PersistenceQuery
import com.github.j5ik2o.akka.persistence.dynamodb.query.scaladsl.DynamoDBReadJournal
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.{
  ActorSpecSupport,
  PersistentThreadAggregate,
  PersistentThreadAggregateOnDynamoDBSpec
}
import com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc.ThreadMessageComponent
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol.Start
import com.github.j5ik2o.threadWeaver.adaptor.util.{
  DynamoDBSpecSupport,
  FlywayWithMySQLSpecSupport,
  Slick3SpecSupport
}
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.typesafe.config.ConfigFactory
import org.scalatest.FreeSpecLike
import org.scalatest.time.{ Seconds, Span }

class ThreadReadModelUpdaterOnDynamoDBSpec
    extends ScalaTestWithActorTestKit(
      ConfigFactory
        .parseString(s"""
           |akka {
           |  persistence {
           |    journal {
           |      plugin = dynamo-db-journal
           |      auto-start-journals = ["dynamo-db-journal"]
           |    }
           |    snapshot-store {
           |      plugin = dynamo-db-snapshot
           |      auto-start-snapshot-stores = ["dynamo-db-snapshot"]
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
           |
           |dynamo-db-read-journal {
           |  dynamodb-client {
           |    access-key-id = "x"
           |    secret-access-key = "x"
           |    endpoint = "http://127.0.0.1:${PersistentThreadAggregateOnDynamoDBSpec.dbPort}/"
           |  }
           |}
      """.stripMargin).withFallback(
          ConfigFactory.load()
        )
    )
    with FreeSpecLike
    with ActorSpecSupport
    with DynamoDBSpecSupport
    with FlywayWithMySQLSpecSupport
    with Slick3SpecSupport {

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(1, Seconds)))

  val tables = Seq("thread")

  var readJournal: DynamoDBReadJournal = _

  override protected lazy val dynamoDBPort: Int = PersistentThreadAggregateOnDynamoDBSpec.dbPort

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createJournalTable()
    createSnapshotTable()
    readJournal = PersistenceQuery(system.toUntyped).readJournalFor[DynamoDBReadJournal](DynamoDBReadJournal.Identifier)
  }

  "ThreadReadModelUpdater" - {
    "add messages" in {
      val threadId          = ThreadId()
      val threadRef         = spawn(PersistentThreadAggregate.behavior(threadId))
      val now               = Instant.now
      val createThreadProbe = TestProbe[CreateThreadResponse]()
      val administratorId   = AccountId()

      threadRef ! CreateThread(
        ULID(),
        threadId,
        administratorId,
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
        administratorId,
        MemberIds(memberId),
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
      val messages                 = Messages(TextMessage(MessageId(), None, ToAccountIds.empty, Text("ABC"), now, now))
      threadRef ! AddMessages(
        ULID(),
        threadId,
        memberId,
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

      new ThreadMessageComponent {

        override val profile = dbConfig.profile
        import profile.api._

        val trmuRef = spawn(new ThreadReadModelUpdater(readJournal, dbConfig.profile, dbConfig.db).behavior)
        trmuRef ! Start(ULID(), threadId, Instant.now)

        eventually {
          val resultMessages =
            dbConfig.db.run(ThreadMessageDao.filter(_.threadId === threadId.value.asString).result).futureValue
          resultMessages should not be empty
          resultMessages should have length 1
        }
      }
    }
  }

}
