package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.persistence.query.PersistenceQuery
import com.github.j5ik2o.akka.persistence.dynamodb.query.scaladsl.DynamoDBReadJournal
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.{
  ActorSpecSupport,
  PersistentThreadAggregate,
  PersistentThreadAggregateOnDynamoDBSpec,
  ThreadProtocol
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

  override val tables = Seq("thread_message", "thread_member_ids", "thread_administrator_ids", "thread")

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
      val threadId = ThreadId()

      val tmc = new ThreadMessageComponent {

        override val profile = dbConfig.profile
        import profile.api._

        val trmuRef = spawn(new ThreadReadModelUpdater(readJournal, dbConfig.profile, dbConfig.db).behavior())

        def assert = eventually {
          val resultMessages =
            dbConfig.db.run(ThreadMessageDao.filter(_.threadId === threadId.value.asString).result).futureValue
          resultMessages should not be empty
          resultMessages should have length 1
          resultMessages.head.body shouldBe "ABC"
        }
      }

      val subscriber = spawn(Behaviors.receiveMessagePartial[ThreadProtocol.Message] {
        case Started(_, tid, _, _) =>
          tmc.trmuRef ! Start(ULID(), tid, Instant.now)
          Behaviors.same
      })

      val threadRef         = spawn(PersistentThreadAggregate.behavior(threadId, Seq(subscriber)))
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

      tmc.assert

    }
  }

}
