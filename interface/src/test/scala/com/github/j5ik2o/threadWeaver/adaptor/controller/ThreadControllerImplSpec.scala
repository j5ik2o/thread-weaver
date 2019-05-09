package com.github.j5ik2o.threadWeaver.adaptor.controller

import java.time.Instant

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.model._
import com.github.j5ik2o.threadWeaver.adaptor.AirframeSettings
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.PersistenceCleanup
import com.github.j5ik2o.threadWeaver.adaptor.json._
import com.github.j5ik2o.threadWeaver.adaptor.util.{ FlywayWithMySQLSpecSupport, Slick3SpecSupport }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import io.circe.generic.auto._
import org.scalatest.FreeSpec
import wvlet.airframe.Design

class ThreadControllerImplSpec
    extends FreeSpec
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

  var controller: ThreadController = _

  override def design: Design = super.design.add(AirframeSettings.designOfSlick(dbConfig.profile, dbConfig.db))

  override def beforeAll(): Unit = {
    deleteStorageLocations()
    super.beforeAll()
    controller = session.build[ThreadController]
  }

  "ThreadControllerImpl" - {
    "create" in {
      val administratorId = ULID().asString
      val entity =
        CreateThreadRequestJson(administratorId, None, Seq(administratorId), Seq.empty, Instant.now.toEpochMilli).toHttpEntity
      Post("/threads", entity) ~> controller.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
      }
    }
    "add administratorIds" in {
      val administratorId = ULID().asString
      val createThread =
        CreateThreadRequestJson(administratorId, None, Seq(administratorId), Seq.empty, Instant.now.toEpochMilli).toHttpEntity
      Post("/threads", createThread) ~> controller.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
        val threadId  = responseJson.id.get
        val accountId = ULID().asString
        val addAdministratorIds =
          AddAdministratorIdsRequestJson(administratorId, Seq(accountId), Instant.now.toEpochMilli).toHttpEntity
        Post(s"/threads/$threadId/administrator-ids/add", addAdministratorIds) ~> controller.addAdministratorIds ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[AddAdministratorIdsResponseJson]
          responseJson.isSuccessful shouldBe true
        }
      }
    }
    "add memberIds" in {
      val administratorId = ULID().asString
      val createThread =
        CreateThreadRequestJson(administratorId, None, Seq(administratorId), Seq.empty, Instant.now.toEpochMilli).toHttpEntity
      Post("/threads", createThread) ~> controller.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
        val threadId  = responseJson.id.get
        val accountId = ULID().asString
        val addMemberIds =
          AddMemberIdsRequestJson(administratorId, Seq(accountId), Instant.now.toEpochMilli).toHttpEntity
        Post(s"/threads/$threadId/member-ids/add", addMemberIds) ~> controller.addMemberIds ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[AddMemberIdsResponseJson]
          responseJson.isSuccessful shouldBe true
        }
      }
    }
    "add messages" in {
      val administratorId = ULID().asString
      val accountId       = ULID().asString
      val createThread =
        CreateThreadRequestJson(administratorId, None, Seq(administratorId), Seq(accountId), Instant.now.toEpochMilli).toHttpEntity
      Post("/threads", createThread) ~> controller.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
        val threadId = responseJson.id.get
        val addMessages =
          AddMessagesRequestJson(accountId, Seq(TextMessage(None, Seq.empty, "ABC")), Instant.now.toEpochMilli).toHttpEntity
        Post(s"/threads/$threadId/messages/add", addMessages) ~> controller.addMessages ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[AddMessagesResponseJson]
          responseJson.isSuccessful shouldBe true
        }
      }
    }
    "remove messages" in {
      val administratorId = ULID().asString
      val accountId       = ULID().asString
      val createThread =
        CreateThreadRequestJson(administratorId, None, Seq(administratorId), Seq(accountId), Instant.now.toEpochMilli).toHttpEntity
      Post("/threads", createThread) ~> controller.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
        val threadId = responseJson.id.get
        val addMessages =
          AddMessagesRequestJson(accountId, Seq(TextMessage(None, Seq.empty, "ABC")), Instant.now.toEpochMilli).toHttpEntity
        Post(s"/threads/$threadId/messages/add", addMessages) ~> controller.addMessages ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[AddMessagesResponseJson]
          responseJson.isSuccessful shouldBe true
          val messageIds = responseJson.ids
          val removeMessagesRequestJson =
            RemoveMessagesRequestJson(accountId, messageIds, Instant.now.toEpochMilli).toHttpEntity
          Post(s"/threads/$threadId/messages/remove", removeMessagesRequestJson) ~> controller.removeMessages ~> check {
            response.status shouldEqual StatusCodes.OK
          }
        }
      }
    }
  }

}
