package com.github.j5ik2o.threadWeaver.adaptor.controller

import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.model.{ HttpEntity, MediaTypes }
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import akka.testkit.TestKit
import akka.util.ByteString
import com.github.j5ik2o.threadWeaver.adaptor.AirframeSettings
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Encoder
import io.circe.syntax._
import kamon.context.Context
import org.scalatest.{ BeforeAndAfterAll, Matchers, TestSuite }
import wvlet.airframe.{ Design, Session }

import scala.concurrent.duration._

trait RouteSpec extends ScalatestRouteTest with Matchers with BeforeAndAfterAll with FailFastCirceSupport {
  this: TestSuite =>

  implicit class ToHttpEntityOps[A: Encoder](json: A) {

    def toHttpEntity: HttpEntity.Strict = {
      val jsonAsByteString = ByteString(json.asJson.noSpaces)
      HttpEntity(MediaTypes.`application/json`, jsonAsByteString)
    }

  }

  implicit def timeout: RouteTestTimeout = RouteTestTimeout(5 seconds)
  implicit val context                   = Context()
  private var _session: Session          = _
  def session: Session                   = _session

  override def beforeAll(): Unit = {
    super.beforeAll()
    val design: Design =
      com.github.j5ik2o.threadWeaver.useCase.AirframeSettings.design
        .add(AirframeSettings.designOfActorSystem(system.toTyped, materializer))
        .add(AirframeSettings.designOfPresenters)
        .add(AirframeSettings.designOfControllers)
        .add(AirframeSettings.designOfLocalAggregatesWithoutPersistence)
    _session = design.newSession
    _session.start
  }

  override def afterAll(): Unit = {
    _session.shutdown
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

}
