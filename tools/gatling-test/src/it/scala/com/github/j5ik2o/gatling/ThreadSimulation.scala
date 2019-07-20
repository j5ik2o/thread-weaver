package com.github.j5ik2o.gatling

import java.util.Date

import com.github.j5ik2o.threadWeaver.adaptor.http.json.CreateThreadRequestJson
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.circe.syntax._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._

class ThreadSimulation extends Simulation {
  private val config         = ConfigFactory.load()
  private val endpoint       = config.getString("thread-weaver.gatling.target-endpoint-base-url")
  private val pauseDuration  = config.getDuration("thread-weaver.gatling.pause-duration").toMillis.millis
  private val numOfUser      = config.getInt("thread-weaver.gatling.users")
  private val rampDuration   = config.getDuration("thread-weaver.gatling.ramp-duration").toMillis.millis
  private val holdDuration   = config.getDuration("thread-weaver.gatling.hold-duration").toMillis.millis
  private val entireDuration = rampDuration + holdDuration

  private val jsonContentHeader = Map(
    "Content-Type" -> "application/json"
  )
  private val httpConf: HttpProtocolBuilder =
    http.userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  private val threadIdKey  = "thread_id"
  private val accountIdKey = "account_id"

  val scn = scenario(getClass.getName)
    .forever {
      feed(randomFeeder)
        .exec(createThread)
        .pause(pauseDuration)
        .exec(getThread)
    }

  setUp(
    scn.inject(rampUsers(numOfUser).during(rampDuration))
  ).protocols(httpConf).maxDuration(entireDuration)

  private def randomFeeder: Iterator[Map[String, String]] =
    Iterator.continually(
      Map(
        accountIdKey -> ULID().asString
      )
    )

  private def getThread: HttpRequestBuilder = {
    http("get-thread")
      .get { session =>
        val threadId  = session(threadIdKey).as[String]
        val accountId = session(accountIdKey).as[String]
        s"$endpoint/threads/$threadId?account_id=$accountId"
      }
      .check(status.is(200))
      .check(jsonPath("$.result.id").find.exists)
  }

  private def createThread: HttpRequestBuilder =
    http("create-thread")
      .post(s"$endpoint/threads/create")
      .headers(jsonContentHeader)
      .body(
        StringBody { session =>
          val accountId = session(accountIdKey).as[String]
          CreateThreadRequestJson(
            accountId = accountId,
            parentThreadId = None,
            title = ULID().asString,
            remarks = None,
            administratorIds = Seq(accountId),
            memberIds = Seq(accountId),
            createAt = new Date().getTime()
          ).asJson.noSpaces
        }
      )
      .check(status.is(200))
      .check(jsonPath("$['thread_id']").find.transform(_.toString).exists.saveAs(threadIdKey))
}
