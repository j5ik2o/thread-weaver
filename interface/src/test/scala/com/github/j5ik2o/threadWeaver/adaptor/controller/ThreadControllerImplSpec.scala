package com.github.j5ik2o.threadWeaver.adaptor.controller

import java.time.Instant

import akka.http.scaladsl.model._
import com.github.j5ik2o.threadWeaver.adaptor.json.{ CreateThreadRequestJson, CreateThreadResponseJson }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import io.circe.generic.auto._
import org.scalatest.FreeSpec

class ThreadControllerImplSpec extends FreeSpec with RouteSpec {

  var controller: ThreadController = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    controller = session.build[ThreadController]
  }

  "ThreadControllerImpl" - {
    "create" in {
      val administratorId = ULID().asString
      val createdAt       = Instant.now.toEpochMilli
      val entity          = CreateThreadRequestJson(None, Seq(administratorId), Seq.empty, createdAt).toHttpEntity
      Post("/threads", entity) ~> controller.createThread ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateThreadResponseJson]
        responseJson.isSuccessful shouldBe true
      }
    }
  }

}
