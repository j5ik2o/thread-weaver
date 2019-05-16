package com.github.j5ik2o.threadWeaver.adaptor.grpc.service

import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ typed, ActorSystem }
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.testkit.TestKit
import com.github.j5ik2o.threadWeaver.adaptor.{ DISettings, DITestSettings }
import org.scalatest.{ BeforeAndAfterAll, TestSuite }
import wvlet.airframe.{ Design, Session }

trait ServiceSpec extends BeforeAndAfterAll {
  this: TestSuite =>

  def system: ActorSystem

  implicit val typedActorSystem: typed.ActorSystem[Nothing] = system.toTyped

  val materializer = ActorMaterializer()

  private var _session: Session = _
  def session: Session          = _session

  def design: Design =
    com.github.j5ik2o.threadWeaver.useCase.DISettings.design
      .add(DISettings.designOfActorSystem(typedActorSystem, materializer))
      .add(
        DISettings.designOfReadJournal(
          PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
        )
      )
      .add(DISettings.designOfGrpcPresenters)
      .add(DISettings.designOfGrpcServices)
      .add(DITestSettings.designOfLocalAggregatesWithPersistence)
      .add(DITestSettings.designOfLocalReadModelUpdater)
      .add(DISettings.designOfMessageRouters)

  override def beforeAll: Unit = {
    super.beforeAll()
    _session = design.newSession
    _session.start
  }

  override def afterAll: Unit = {
    _session.shutdown
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

}
