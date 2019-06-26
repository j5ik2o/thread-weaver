package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.actor.ActorSystem
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.testkit.{ ImplicitSender, TestKit }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.PersistenceCleanup
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.PersistentThreadAggregate
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.PersistentThreadAggregate.ReadModelUpdaterConfig
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc.ThreadMessageComponent
import com.github.j5ik2o.threadWeaver.adaptor.util.{ FlywayWithMySQLSpecSupport, Slick3SpecSupport }
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.{ FreeSpecLike, Matchers }

class ThreadReadModelUpdaterOnLevelDBSpec
    extends TestKit(
      ActorSystem(
        "ThreadReadModelUpdaterOnLevelDBSpec",
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
          |      auto-start-journals = ["akka.persistence.journal.leveldb"]
          |    }
          |    snapshot-store {
          |      plugin = akka.persistence.snapshot-store.local
          |      local.dir = "target/persistence/snapshots"
          |      auto-start-snapshot-stores = ["akka.persistence.snapshot-store.local"]
          |    }
          |  }
          |}
        """.stripMargin).withFallback(
            ConfigFactory.load()
          )
      )
    )
    with ImplicitSender
    with Matchers
    with Eventually
    with FreeSpecLike
    with PersistenceCleanup
    with FlywayWithMySQLSpecSupport
    with Slick3SpecSupport {

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(1, Seconds)))

  override val tables = Seq("thread_message", "thread_member_ids", "thread_administrator_ids", "thread")

  var readJournal: LeveldbReadJournal = _

  override def beforeAll: Unit = {
    deleteStorageLocations(system)
    super.beforeAll()
    readJournal = PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
  }

  override def afterAll: Unit = {
    deleteStorageLocations(system)
    super.afterAll()
  }

  "ThreadReadModelUpdater" - {
    "add messages" in {
      val threadId = ThreadId()

      val tmc = new ThreadMessageComponent {

        override val profile = dbConfig.profile
        import profile.api._

        def assert = eventually {
          val resultMessages =
            dbConfig.db.run(ThreadMessageDao.filter(_.threadId === threadId.value.asString).result).futureValue
          resultMessages should not be empty
          resultMessages should have length 1
          resultMessages.head.body shouldBe "ABC"
        }
      }

      val threadRef = system.actorOf(
        PersistentThreadAggregate.props(Some(ReadModelUpdaterConfig(readJournal, dbConfig.profile, dbConfig.db, 1)))(
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

      tmc.assert

    }
  }

}
