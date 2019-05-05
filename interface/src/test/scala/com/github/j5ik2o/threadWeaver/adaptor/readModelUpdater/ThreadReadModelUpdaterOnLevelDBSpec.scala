package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.scaladsl.adapter._
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.{
  ActorSpecSupport,
  PersistenceCleanup,
  PersistentThreadAggregate
}
import com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc.ThreadMessageComponent
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol.Start
import com.github.j5ik2o.threadWeaver.adaptor.util.{ FlywayWithMySQLSpecSupport, Slick3SpecSupport }
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.typesafe.config.ConfigFactory
import org.scalatest.FreeSpecLike
import org.scalatest.time.{ Seconds, Span }

class ThreadReadModelUpdaterOnLevelDBSpec
    extends ScalaTestWithActorTestKit(
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
    with FreeSpecLike
    with ActorSpecSupport
    with PersistenceCleanup
    with FlywayWithMySQLSpecSupport
    with Slick3SpecSupport {

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(1, Seconds)))

  val tables = Seq("thread")

  var readJournal: LeveldbReadJournal = _

  override protected def beforeAll(): Unit = {
    deleteStorageLocations()
    super.beforeAll()
    readJournal = PersistenceQuery(system.toUntyped).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
  }

  override protected def afterAll(): Unit = {
    deleteStorageLocations()
    super.afterAll()
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

      java.lang.Thread.sleep(1000 * 3)

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
