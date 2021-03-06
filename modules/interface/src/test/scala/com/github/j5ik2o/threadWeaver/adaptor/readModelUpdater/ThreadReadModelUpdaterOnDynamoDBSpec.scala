package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.actor.ActorSystem
import akka.persistence.query.PersistenceQuery
import akka.testkit.{ ImplicitSender, TestKit }
import com.github.j5ik2o.akka.persistence.dynamodb.query.scaladsl.DynamoDBReadJournal
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.PersistentThreadAggregate
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc.{ ThreadComponent, ThreadMessageComponent }
import com.github.j5ik2o.threadWeaver.adaptor.util.{
  DynamoDBSpecSupport,
  FlywayWithMySQLSpecSupport,
  RandomPortSupport,
  Slick3SpecSupport
}
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.typesafe.config.ConfigFactory
import org.scalatest.FreeSpecLike
import org.scalatest.time.{ Seconds, Span }

object ThreadReadModelUpdaterOnDynamoDBSpec extends RandomPortSupport {
  lazy val dbPort: Int = temporaryServerPort()
}

class ThreadReadModelUpdaterOnDynamoDBSpec
    extends TestKit(
      ActorSystem(
        "ThreadReadModelUpdaterOnDynamoDBSpec",
        ConfigFactory
          .parseString(s"""
           |akka {
           |  persistence {
           |    journal {
           |      plugin = j5ik2o.dynamo-db-journal
           |    }
           |    snapshot-store {
           |      plugin = j5ik2o.dynamo-db-snapshot
           |    }
           |  }
           |}
           |thread-weaver {
           |  read-model-updater.thread {
           |    shard-name = "thread"
           |    category = "thread"
           |    num-partition = 1
           |  }
           |}
           |j5ik2o {
           |  dynamo-db-journal {
           |    event-adapters {
           |      thread = "com.github.j5ik2o.threadWeaver.adaptor.serialization.ThreadTaggingEventAdaptor"
           |    }
           |    event-adapter-bindings {
           |      "com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadCommonProtocol$$Event" = [thread]
           |    } 
           |    dynamo-db-client {
           |      access-key-id = "x"
           |      secret-access-key = "x"
           |      endpoint = "http://127.0.0.1:${ThreadReadModelUpdaterOnDynamoDBSpec.dbPort}/"
           |    }
           |  }
           |
           |  dynamo-db-snapshot {
           |    dynamo-db-client {
           |      access-key-id = "x"
           |      secret-access-key = "x"
           |      endpoint = "http://127.0.0.1:${ThreadReadModelUpdaterOnDynamoDBSpec.dbPort}/"
           |    }
           |  }
           |
           |  dynamo-db-read-journal {
           |    dynamo-db-client {
           |      access-key-id = "x"
           |      secret-access-key = "x"
           |      endpoint = "http://127.0.0.1:${ThreadReadModelUpdaterOnDynamoDBSpec.dbPort}/"
           |    }
           |  }
           |}
      """.stripMargin).withFallback(
            ConfigFactory.load()
          )
      )
    )
    with ImplicitSender
    with FreeSpecLike
    with DynamoDBSpecSupport
    with FlywayWithMySQLSpecSupport
    with Slick3SpecSupport {

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(1, Seconds)))

  override val tables = Seq("thread_message", "thread_member_ids", "thread_administrator_ids", "thread")

  var readJournal: DynamoDBReadJournal = _

  override protected lazy val dynamoDBPort: Int = ThreadReadModelUpdaterOnDynamoDBSpec.dbPort

  override def beforeAll: Unit = {
    super.beforeAll()
    createJournalTable()
    createSnapshotTable()
    readJournal = PersistenceQuery(system).readJournalFor[DynamoDBReadJournal](DynamoDBReadJournal.Identifier)
  }

  "ThreadReadModelUpdater" - {
    "add messages" in {
      val threadId = ThreadId()

      val tmc = new ThreadMessageComponent with ThreadComponent {

        override val profile = dbConfig.profile
        import profile.api._

        def assert = eventually {
          val resultThreads = dbConfig.db.run(ThreadDao.filter(_.id === threadId.value.asString).result).futureValue
          resultThreads.head.persistenceTag shouldBe "thread-0"
          val resultMessages =
            dbConfig.db.run(ThreadMessageDao.filter(_.threadId === threadId.value.asString).result).futureValue
          resultMessages should not be empty
          resultMessages should have length 1
          resultMessages.head.body shouldBe "ABC"
        }
      }

      implicit val config = system.settings.config

      val rmu = system.actorOf(ThreadReadModelUpdater.props(readJournal, dbConfig.profile, dbConfig.db))
      rmu ! ThreadReadModelUpdaterProtocol.Start(ULID(), ThreadTag.fromThreadId(threadId), Instant.now())

      val threadRef = system.actorOf(
        PersistentThreadAggregate.props(
          threadId
        )(Seq.empty)
      )

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

      tmc.assert

    }
  }

}
