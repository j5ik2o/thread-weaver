package com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater

import akka.actor.{ Actor, Props }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.BaseCommandRequest
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ChildActorLookup
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import slick.jdbc.JdbcProfile

class ThreadReadModelUpdaters(
    readJournal: ReadJournalType,
    profile: JdbcProfile,
    db: JdbcProfile#Backend#Database,
    sqlBatchSize: Long
) extends Actor
    with ChildActorLookup {
  override type ID = ThreadTag
  override def receive: Receive = forwardToActor

  override protected def childName(childId: ThreadTag): String = childId.value

  override protected def childProps(childId: ThreadTag): Props =
    ThreadReadModelUpdater.props(readJournal, profile, db, sqlBatchSize)

  override protected def toChildId(commandRequest: BaseCommandRequest): ThreadTag =
    commandRequest.asInstanceOf[ThreadReadModelUpdaterProtocol.CommandRequest].threadTag

}
