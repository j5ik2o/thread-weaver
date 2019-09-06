package com.github.j5ik2o.threadWeaver.adaptor.grpc.service
import java.time.Instant

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.github.j5ik2o.threadWeaver.adaptor.DISettings
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model._
import com.github.j5ik2o.threadWeaver.adaptor.util._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.{ FreeSpecLike, Matchers }
import wvlet.airframe.Design

object ThreadServiceImplSpec extends RandomPortSupport {
  val dynamoDBPort: Int = temporaryServerPort()
}

class ThreadServiceImplSpec
    extends TestKit(
      ActorSystem(
        "ThreadServiceImplSpec",
        ConfigFactory
          .parseString(
            s"""
          |thread-weaver.read-model-updater.thread { 
          |  shard-name = "thread"
          |  category = "thread"
          |  num-partition = 1
          |}
          |
          |akka.persistence.journal.plugin = "j5ik2o.dynamo-db-journal"
          |akka.persistence.snapshot-store.plugin = "j5ik2o.dynamo-db-snapshot"
          |
          |j5ik2o {
          |  dynamo-db-journal {
          |    class = "com.github.j5ik2o.akka.persistence.dynamodb.journal.DynamoDBJournal"
          |    plugin-dispatcher = "akka.actor.default-dispatcher"
          |    event-adapters {
          |      thread = "com.github.j5ik2o.threadWeaver.adaptor.serialization.ThreadTaggingEventAdaptor"
          |    }
          |    event-adapter-bindings {
          |      "com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadCommonProtocol$$Event" = [thread]
          |    }
          |    dynamo-db-client {
          |      access-key-id = "x"
          |      secret-access-key = "x"
          |      endpoint = "http://127.0.0.1:${ThreadServiceImplSpec.dynamoDBPort}/"
          |    }
          |  }
          |
          |  dynamo-db-snapshot {
          |    dynamo-db-client {
          |      access-key-id = "x"
          |      secret-access-key = "x"
          |      endpoint = "http://127.0.0.1:${ThreadServiceImplSpec.dynamoDBPort}/"
          |    }
          |  }
          |
          |  dynamo-db-read-journal {
          |    dynamo-db-client {
          |      access-key-id = "x"
          |      secret-access-key = "x"
          |      endpoint = "http://127.0.0.1:${ThreadServiceImplSpec.dynamoDBPort}/"
          |    }
          |  }
          |}
        """.stripMargin
          ).withFallback(
            ConfigFactory.load()
          )
      )
    )
    with FreeSpecLike
    with Eventually
    with ScalaFuturesSpecSupport
    with DynamoDBSpecSupport
    with FlywayWithMySQLSpecSupport
    with Slick3SpecSupport
    with ServiceSpec
    with Matchers {
  override protected lazy val dynamoDBPort: Int = ThreadServiceImplSpec.dynamoDBPort
  override val tables: Seq[String]              = Seq.empty

  var threadCommandService: ThreadCommandService = _
  var threadQueryService: ThreadQueryService     = _

  override def design: Design = super.design.add(DISettings.designOfSlick(dbConfig.profile, dbConfig.db))

  override def beforeAll: Unit = {
    startDynamoDBLocal()
    createJournalTable()
    createSnapshotTable()
    super.beforeAll()
    threadCommandService = session.build[ThreadCommandService]
    threadQueryService = session.build[ThreadQueryService]
  }

  "ThreadControllerImpl" - {
    "create" in {
      val administratorId = ULID().asString
      val now             = Instant.now.toEpochMilli
      val createThread =
        CreateThreadRequest.of(
          accountId = administratorId,
          hasParentId = false,
          parentId = "",
          title = "test",
          hasRemarks = false,
          remarks = "",
          administratorIds = Seq(administratorId),
          memberIds = Seq.empty,
          createAt = now
        )
      val createResponse = threadCommandService.createThread(createThread).futureValue
      createResponse.isSuccessful shouldBe true
      createResponse.threadId.nonEmpty shouldBe true
      eventually {
        val getThread         = GetThreadRequest.of(createResponse.threadId, administratorId)
        val getThreadResponse = threadQueryService.getThread(getThread).futureValue
        getThreadResponse.isSuccessful shouldBe true
      }
    }
    "destroy" in {
      val administratorId = ULID().asString
      val now             = Instant.now.toEpochMilli
      val createThread =
        CreateThreadRequest.of(
          accountId = administratorId,
          hasParentId = false,
          parentId = "",
          title = "test",
          hasRemarks = false,
          remarks = "",
          administratorIds = Seq(administratorId),
          memberIds = Seq.empty,
          createAt = now
        )
      val createResponse = threadCommandService.createThread(createThread).futureValue
      createResponse.isSuccessful shouldBe true
      val threadId = createResponse.threadId
      threadId.nonEmpty shouldBe true
      eventually {
        val getThread         = GetThreadRequest.of(threadId, administratorId)
        val getThreadResponse = threadQueryService.getThread(getThread).futureValue
        getThreadResponse.isSuccessful shouldBe true
      }
      val destroyThread         = DestroyThreadRequest.of(administratorId, threadId, now)
      val destroyThreadResponse = threadCommandService.destroyThread(destroyThread).futureValue
      destroyThreadResponse.isSuccessful shouldBe true
    }
    "join administratorIds" in {
      val administratorId = ULID().asString
      val now             = Instant.now.toEpochMilli
      val createThread =
        CreateThreadRequest.of(
          accountId = administratorId,
          hasParentId = false,
          parentId = "",
          title = "test",
          hasRemarks = false,
          remarks = "",
          administratorIds = Seq(administratorId),
          memberIds = Seq.empty,
          createAt = now
        )
      val createResponse = threadCommandService.createThread(createThread).futureValue
      createResponse.isSuccessful shouldBe true
      createResponse.threadId.nonEmpty shouldBe true
      val threadId = createResponse.threadId

      val joinAdministratorIds         = JoinAdministratorIdsRequest(administratorId, threadId, Seq(ULID().asString), now)
      val joinAdministratorIdsResponse = threadCommandService.joinAdministratorIds(joinAdministratorIds).futureValue
      joinAdministratorIdsResponse.isSuccessful shouldBe true
    }
    "join mebmerIds" in {
      val administratorId = ULID().asString
      val now             = Instant.now.toEpochMilli
      val createThread =
        CreateThreadRequest.of(
          accountId = administratorId,
          hasParentId = false,
          parentId = "",
          title = "test",
          hasRemarks = false,
          remarks = "",
          administratorIds = Seq(administratorId),
          memberIds = Seq.empty,
          createAt = now
        )
      val createResponse = threadCommandService.createThread(createThread).futureValue
      createResponse.isSuccessful shouldBe true
      createResponse.threadId.nonEmpty shouldBe true
      val threadId = createResponse.threadId

      val joinMemberIds         = JoinMemberIdsRequest.of(administratorId, threadId, Seq(ULID().asString), now)
      val joinMemberIdsResponse = threadCommandService.joinMemberIds(joinMemberIds).futureValue
      joinMemberIdsResponse.isSuccessful shouldBe true
    }
    "add messages" ignore {
      val administratorId = ULID().asString
      val now             = Instant.now.toEpochMilli
      val createThread =
        CreateThreadRequest.of(
          accountId = administratorId,
          hasParentId = false,
          parentId = "",
          title = "test",
          hasRemarks = false,
          remarks = "",
          administratorIds = Seq(administratorId),
          memberIds = Seq.empty,
          createAt = now
        )
      val createResponse = threadCommandService.createThread(createThread).futureValue
      createResponse.isSuccessful shouldBe true
      createResponse.threadId.nonEmpty shouldBe true
      val threadId = createResponse.threadId

      val addMessages = AddMessagesRequest.of(
        administratorId,
        threadId,
        messages = Seq(Message(hasReplyMessageId = false, "", Seq.empty, "ABC")),
        createAt = now
      )
      val addMessagesResponse = threadCommandService.addMessages(addMessages).futureValue
      addMessagesResponse.isSuccessful shouldBe true
      addMessagesResponse.messageIds.nonEmpty shouldBe true

      eventually {
        val getMessages         = GetMessagesRequest.of(administratorId, threadId, false, 0L, false, 0L, now)
        val getMessagesResponse = threadQueryService.getMessages(getMessages).futureValue
        getMessagesResponse.isSuccessful shouldBe true
        getMessagesResponse.messages.length shouldBe 1
      }
    }
  }

}
