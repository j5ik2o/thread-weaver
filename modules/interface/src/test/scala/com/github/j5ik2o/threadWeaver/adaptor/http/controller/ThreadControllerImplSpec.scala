package com.github.j5ik2o.threadWeaver.adaptor.http.controller

import java.time.Instant

import akka.http.scaladsl.model._
import com.github.j5ik2o.threadWeaver.adaptor.DISettings
import com.github.j5ik2o.threadWeaver.adaptor.http.json._
import com.github.j5ik2o.threadWeaver.adaptor.http.routes.RouteNames
import com.github.j5ik2o.threadWeaver.adaptor.util._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import io.circe.generic.auto._
import kamon.Kamon
import kamon.trace.TraceContext
import org.scalatest.FreeSpec
import org.scalatest.concurrent.Eventually
import wvlet.airframe.Design

object ThreadControllerImplSpec extends RandomPortSupport {
  val dynamoDBPort: Int = temporaryServerPort()
}

class ThreadControllerImplSpec
    extends FreeSpec
    with Eventually
    with DynamoDBSpecSupport
    with ScalaFuturesSpecSupport
    with FlywayWithMySQLSpecSupport
    with Slick3SpecSupport
    with RouteSpec {

  override def testConfigSource: String =
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
      |      endpoint = "http://127.0.0.1:${ThreadControllerImplSpec.dynamoDBPort}/"
      |    }
      |  }
      |
      |  dynamo-db-snapshot {
      |    class = "com.github.j5ik2o.akka.persistence.dynamodb.snapshot.DynamoDBSnapshotStore"
      |    plugin-dispatcher = "akka.actor.default-dispatcher"
      |    dynamo-db-client {
      |      access-key-id = "x"
      |      secret-access-key = "x"
      |      endpoint = "http://127.0.0.1:${ThreadControllerImplSpec.dynamoDBPort}/"
      |    }
      |  }
      |
      |  dynamo-db-read-journal {
      |    class = "com.github.j5ik2o.akka.persistence.dynamodb.query.DynamoDBReadJournalProvider"
      |    write-plugin = "j5ik2o.dynamo-db-journal"
      |    dynamo-db-client {
      |      access-key-id = "x"
      |      secret-access-key = "x"
      |      endpoint = "http://127.0.0.1:${ThreadControllerImplSpec.dynamoDBPort}/"
      |    }
      |  }
      |}
    """.stripMargin
  override protected lazy val dynamoDBPort: Int = ThreadControllerImplSpec.dynamoDBPort
  override val tables: Seq[String]              = Seq.empty

  implicit val traceContext: TraceContext = Kamon.tracer.newContext("default")

  var commandController: ThreadCommandController = _
  var queryController: ThreadQueryController     = _

  override def design: Design =
    super.design
      .add(DISettings.designOfSlick(dbConfig.profile, dbConfig.db))

  override def beforeAll(): Unit = {
    startDynamoDBLocal()
    createJournalTable()
    createSnapshotTable()
    super.beforeAll()
    commandController = session.build[ThreadCommandController]
    queryController = session.build[ThreadQueryController]
  }

  override def afterAll(): Unit = {
    super.afterAll()
    shutdownDynamoDBLocal()
  }

  "ThreadControllerImpl" - {
    "create" in {
      val administratorId = ULID().asString
      val entity =
        CreateThreadRequestJson(
          administratorId,
          None,
          "test",
          None,
          Seq(administratorId),
          Seq.empty,
          Instant.now.toEpochMilli
        ).toHttpEntity
      Post(RouteNames.CreateThread, entity) ~> commandController.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
        val threadId = responseJson.thread_id.get
        eventually {
          Get(RouteNames.GetThread(threadId, administratorId)) ~> queryController.getThread ~> check {
            response.status shouldEqual StatusCodes.OK
            val responseJson = responseAs[GetThreadResponseJson]
            responseJson.isSuccessful shouldBe true
          }
        }
      }
    }
    "destroy" in {
      val administratorId = ULID().asString
      val createThreadRequestJson =
        CreateThreadRequestJson(
          administratorId,
          None,
          "test",
          None,
          Seq(administratorId),
          Seq.empty,
          Instant.now.toEpochMilli
        ).toHttpEntity
      Post(RouteNames.CreateThread, createThreadRequestJson) ~> commandController.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
        val threadId = responseJson.thread_id.get
//        eventually {
//          Get(RouteNames.GetThread(threadId, administratorId)) ~> queryController.getThread ~> check {
//            response.status shouldEqual StatusCodes.OK
//            val responseJson = responseAs[GetThreadResponseJson]
//            responseJson.isSuccessful shouldBe true
//          }
//        }
        val destroyThreadRequestJson =
          DestroyThreadRequestJson(
            administratorId,
            Instant.now.toEpochMilli
          ).toHttpEntity
        Post(RouteNames.DestroyThread(threadId), destroyThreadRequestJson) ~> commandController.destroyThread ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[DestroyThreadResponseJson]
          responseJson.isSuccessful shouldBe true
        }
      }
    }
    "join administratorIds" in {
      val administratorId = ULID().asString
      val createThread =
        CreateThreadRequestJson(
          administratorId,
          None,
          "test",
          None,
          Seq(administratorId),
          Seq.empty,
          Instant.now.toEpochMilli
        ).toHttpEntity
      Post(RouteNames.CreateThread, createThread) ~> commandController.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
        val threadId  = responseJson.thread_id.get
        val accountId = ULID().asString
        val joinAdministratorIds =
          JoinAdministratorIdsRequestJson(administratorId, Seq(accountId), Instant.now.toEpochMilli).toHttpEntity
        Post(RouteNames.JoinAdministratorIds(threadId, administratorId), joinAdministratorIds) ~> commandController.joinAdministratorIds ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[JoinAdministratorIdsResponseJson]
          responseJson.isSuccessful shouldBe true
          eventually {
            Get(RouteNames.GetAdministratorIds(threadId, administratorId)) ~> queryController.getAdministratorIds ~> check {
              response.status shouldEqual StatusCodes.OK
              val responseJson = responseAs[GetThreadAdministratorIdsResponseJson]
              responseJson.isSuccessful shouldBe true
            }
          }
        }
      }
    }
    "join memberIds" in {
      val administratorId = ULID().asString
      val createThread =
        CreateThreadRequestJson(
          administratorId,
          None,
          "test",
          None,
          Seq(administratorId),
          Seq.empty,
          Instant.now.toEpochMilli
        ).toHttpEntity
      Post(RouteNames.CreateThread, createThread) ~> commandController.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
        val threadId  = responseJson.thread_id.get
        val accountId = ULID().asString
        val joinMemberIds =
          JoinMemberIdsRequestJson(administratorId, Seq(accountId), Instant.now.toEpochMilli).toHttpEntity
        Post(RouteNames.JoinMemberIds(threadId), joinMemberIds) ~> commandController.joinMemberIds ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[LeaveMemberIdsResponseJson]
          responseJson.isSuccessful shouldBe true
          eventually {
            Get(RouteNames.GetMemberIds(threadId, administratorId)) ~> queryController.getMemberIds ~> check {
              response.status shouldEqual StatusCodes.OK
              val responseJson = responseAs[GetThreadMemberIdsResponseJson]
              responseJson.isSuccessful shouldBe true
            }
          }
        }
      }
    }
    "add messages" in {
      val administratorId = ULID().asString
      val accountId       = ULID().asString
      val createThread =
        CreateThreadRequestJson(
          administratorId,
          None,
          "test",
          None,
          Seq(administratorId),
          Seq(accountId),
          Instant.now.toEpochMilli
        ).toHttpEntity
      Post(RouteNames.CreateThread, createThread) ~> commandController.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
        val threadId = responseJson.thread_id.get
        val addMessages =
          AddMessagesRequestJson(accountId, Seq(TextMessage(None, Seq.empty, "ABC")), Instant.now.toEpochMilli).toHttpEntity
        Post(RouteNames.AddMessages(threadId), addMessages) ~> commandController.addMessages ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[AddMessagesResponseJson]
          responseJson.isSuccessful shouldBe true
          eventually {
            Get(RouteNames.GetMessages(threadId, administratorId)) ~> queryController.getMessages ~> check {
              response.status shouldEqual StatusCodes.OK
              val responseJson = responseAs[GetThreadMessagesResponseJson]
              responseJson.isSuccessful shouldBe true
            }
          }
        }
      }
    }
    "remove messages" in {
      val administratorId = ULID().asString
      val accountId       = ULID().asString
      val createThread =
        CreateThreadRequestJson(
          administratorId,
          None,
          "test",
          None,
          Seq(administratorId),
          Seq(accountId),
          Instant.now.toEpochMilli
        ).toHttpEntity
      Post(RouteNames.CreateThread, createThread) ~> commandController.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
        val threadId = responseJson.thread_id.get
        val addMessages =
          AddMessagesRequestJson(accountId, Seq(TextMessage(None, Seq.empty, "ABC")), Instant.now.toEpochMilli).toHttpEntity
        Post(s"/threads/$threadId/messages/add", addMessages) ~> commandController.addMessages ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[AddMessagesResponseJson]
          responseJson.isSuccessful shouldBe true
          val messageIds = responseJson.messageIds
          val removeMessagesRequestJson =
            RemoveMessagesRequestJson(accountId, messageIds, Instant.now.toEpochMilli).toHttpEntity
          Post(s"/threads/$threadId/messages/remove", removeMessagesRequestJson) ~> commandController.removeMessages ~> check {
            response.status shouldEqual StatusCodes.OK
          }
        }
      }
    }
  }

}
