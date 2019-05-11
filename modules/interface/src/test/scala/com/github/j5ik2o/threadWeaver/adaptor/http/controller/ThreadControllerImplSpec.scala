package com.github.j5ik2o.threadWeaver.adaptor.http.controller

import java.time.Instant

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.model._
import com.github.j5ik2o.threadWeaver.adaptor.DISettings
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.PersistenceCleanup
import com.github.j5ik2o.threadWeaver.adaptor.http.json._
import com.github.j5ik2o.threadWeaver.adaptor.http.routes.RouteNames
import com.github.j5ik2o.threadWeaver.adaptor.util.{
  FlywayWithMySQLSpecSupport,
  ScalaFuturesSpecSupport,
  Slick3SpecSupport
}
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import io.circe.generic.auto._
import org.scalatest.FreeSpec
import org.scalatest.concurrent.Eventually
import wvlet.airframe.Design

class ThreadControllerImplSpec
    extends FreeSpec
    with Eventually
    with ScalaFuturesSpecSupport
    with FlywayWithMySQLSpecSupport
    with Slick3SpecSupport
    with RouteSpec
    with PersistenceCleanup {

  override def typedSystem: ActorSystem[Nothing] = system.toTyped

  override def testConfigSource: String =
    """
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
    """.stripMargin

  override val tables: Seq[String] = Seq.empty

  var commandController: ThreadCommandController = _
  var queryController: ThreadQueryController     = _

  override def design: Design = super.design.add(DISettings.designOfSlick(dbConfig.profile, dbConfig.db))

  override def beforeAll(): Unit = {
    deleteStorageLocations()
    super.beforeAll()
    commandController = session.build[ThreadCommandController]
    queryController = session.build[ThreadQueryController]
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
        val threadId = responseJson.threadId.get
        eventually {
          Get(RouteNames.GetThread(threadId)) ~> queryController.getThread ~> check {
            response.status shouldEqual StatusCodes.OK
            val responseJson = responseAs[GetThreadResponseJson]
            responseJson.isSuccessful shouldBe true
          }
        }
      }
    }
    "add administratorIds" in {
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
        val threadId  = responseJson.threadId.get
        val accountId = ULID().asString
        val addAdministratorIds =
          JoinAdministratorIdsRequestJson(administratorId, Seq(accountId), Instant.now.toEpochMilli).toHttpEntity
        Post(RouteNames.JoinAdministratorIds(threadId), addAdministratorIds) ~> commandController.joinAdministratorIds ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[JoinAdministratorIdsResponseJson]
          responseJson.isSuccessful shouldBe true
          eventually {
            Get(RouteNames.GetAdministratorIds(threadId)) ~> queryController.getAdministratorIds ~> check {
              response.status shouldEqual StatusCodes.OK
              val responseJson = responseAs[GetThreadAdministratorIdsResponseJson]
              responseJson.isSuccessful shouldBe true
            }
          }
        }
      }
    }
    "add memberIds" in {
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
        val threadId  = responseJson.threadId.get
        val accountId = ULID().asString
        val addMemberIds =
          AddMemberIdsRequestJson(administratorId, Seq(accountId), Instant.now.toEpochMilli).toHttpEntity
        Post(RouteNames.JoinMemberIds(threadId), addMemberIds) ~> commandController.joinMemberIds ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[AddMemberIdsResponseJson]
          responseJson.isSuccessful shouldBe true
          eventually {
            Get(RouteNames.GetMemberIds(threadId)) ~> queryController.getMemberIds ~> check {
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
        val threadId = responseJson.threadId.get
        val addMessages =
          AddMessagesRequestJson(accountId, Seq(TextMessage(None, Seq.empty, "ABC")), Instant.now.toEpochMilli).toHttpEntity
        Post(RouteNames.AddMessages(threadId), addMessages) ~> commandController.addMessages ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[AddMessagesResponseJson]
          responseJson.isSuccessful shouldBe true
          eventually {
            Get(RouteNames.GetMessages(threadId)) ~> queryController.getMessages ~> check {
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
        val threadId = responseJson.threadId.get
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
