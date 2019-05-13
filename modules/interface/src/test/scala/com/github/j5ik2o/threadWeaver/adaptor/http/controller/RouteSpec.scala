package com.github.j5ik2o.threadWeaver.adaptor.http.controller

import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.model.{ HttpEntity, MediaTypes }
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.testkit.TestKit
import akka.util.ByteString
import com.github.j5ik2o.threadWeaver.adaptor.{ DISettings, DITestSettings }
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

  def design: Design =
    com.github.j5ik2o.threadWeaver.useCase.DISettings.design
      .add(DISettings.designOfActorSystem(system.toTyped, materializer))
      .add(
        DISettings.designOfReadJournal(
          PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
        )
      )
      .add(DISettings.designOfRestPresenters)
      .add(DISettings.designOfRestControllers)
      .add(DITestSettings.designOfLocalAggregatesWithPersistence)
      .add(DITestSettings.designOfLocalReadModelUpdater)
      .add(DISettings.designOfMessageRouters)

  override def beforeAll(): Unit = {
    super.beforeAll()
    _session = design.newSession
    _session.start
  }

  override def afterAll(): Unit = {
    _session.shutdown
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

}
