package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.{
  ActorSpecSupport,
  PersistenceCleanup,
  PersistentThreadAggregate,
  ThreadProtocol
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

  override def typedSystem: ActorSystem[Nothing] = system

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(1, Seconds)))

  override val tables = Seq("thread_message", "thread_member_ids", "thread_administrator_ids", "thread")

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

      tmc.assert

    }
  }

}
