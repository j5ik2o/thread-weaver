package com.github.j5ik2o.threadWeaver.adaptor.das

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc.{
  ThreadAdministratorIdsComponent,
  ThreadComponent,
  ThreadMemberIdsComponent,
  ThreadMessageComponent
}
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import slick.jdbc.JdbcProfile
import wvlet.airframe._

trait ThreadDas
    extends ThreadComponent
    with ThreadAdministratorIdsComponent
    with ThreadMemberIdsComponent
    with ThreadMessageComponent {

  override val profile: JdbcProfile    = bind[JdbcProfile]
  val db: JdbcProfile#Backend#Database = bind[JdbcProfile#Backend#Database]

  import profile.api._

  def getThreadByIdSource(threadId: ThreadId): Source[ThreadRecord, NotUsed] = {
    val q = ThreadDao.filter(_.id === threadId.value.asString).result
    Source.fromPublisher(db.stream(q)).take(1)
  }

  def getThreadsByAccountIdSource(
      accountId: AccountId,
      offset: Option[Long],
      limit: Option[Long]
  ): Source[ThreadRecord, NotUsed] = {
    val q = (for {
      ((t, tm), ta) <- (ThreadDao join ThreadMemberIdsDao on (_.id === _.threadId)) join ThreadAdministratorIdsDao on {
        case ((t, _), ta) =>
          t.id === ta.threadId
      }
    } yield (t, tm, ta))
      .filter {
        case (_, tm, ta) =>
          tm.accountId === accountId.value.asString || ta.accountId === accountId.value.asString
      }.map { case (t, _, _) => t }.result
    Source.fromPublisher(db.stream(q)).drop(offset.getOrElse(0)).take(limit.getOrElse(100))
  }

  def getAdministratorsByThreadIdSource(
      threadId: ThreadId,
      offset: Option[Long],
      limit: Option[Long]
  ): Source[ThreadAdministratorIdsRecord, NotUsed] = {
    val q = ThreadAdministratorIdsDao.filter(_.threadId === threadId.value.asString).result
    Source.fromPublisher(db.stream(q)).drop(offset.getOrElse(0)).take(limit.getOrElse(100))
  }

  def getMembersByThreadIdSource(
      threadId: ThreadId,
      offset: Option[Long],
      limit: Option[Long]
  ): Source[ThreadMemberIdsRecord, NotUsed] = {
    val q = ThreadMemberIdsDao.filter(_.threadId === threadId.value.asString).result
    Source.fromPublisher(db.stream(q)).drop(offset.getOrElse(0)).take(limit.getOrElse(100))
  }

  def getMessagesByThreadIdSource(
      threadId: ThreadId,
      offset: Option[Long],
      limit: Option[Long]
  ): Source[ThreadMessageRecord, NotUsed] = {
    val q = ThreadMessageDao.filter(_.threadId === threadId.value.asString).result
    Source.fromPublisher(db.stream(q)).drop(offset.getOrElse(0)).take(limit.getOrElse(100))
  }

}
