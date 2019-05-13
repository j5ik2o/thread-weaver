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

  def getThreadByIdSource(accountId: AccountId, threadId: ThreadId): Source[ThreadRecord, NotUsed] = {
    val q = (for {
      ((t, tm), ta) <- (ThreadDao join ThreadMemberIdsDao on (_.id === _.threadId)) join ThreadAdministratorIdsDao on {
        case ((t, _), ta) =>
          t.id === ta.threadId
      }
    } yield (t, tm, ta))
      .filter {
        case (t, tm, ta) =>
          (tm.accountId === accountId.value.asString || ta.accountId === accountId.value.asString) && t.id === threadId.value.asString
      }.map { case (t, _, _) => t }.result
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
      accountId: AccountId,
      threadId: ThreadId,
      offset: Option[Long],
      limit: Option[Long]
  ): Source[ThreadAdministratorIdsRecord, NotUsed] = {
    val q = (for {
      (t, ta) <- ThreadDao join ThreadAdministratorIdsDao on { case (t, ta) => t.id === ta.threadId }
    } yield (t, ta))
      .filter {
        case (t, ta) =>
          ta.accountId === accountId.value.asString && t.id === threadId.value.asString
      }.map { case (_, ta) => ta }.result
    Source.fromPublisher(db.stream(q)).drop(offset.getOrElse(0)).take(limit.getOrElse(100))
  }

  def getMembersByThreadIdSource(
      accountId: AccountId,
      threadId: ThreadId,
      offset: Option[Long],
      limit: Option[Long]
  ): Source[ThreadMemberIdsRecord, NotUsed] = {
    val q = (for {
      (t, tm) <- ThreadDao join ThreadMemberIdsDao on (_.id === _.threadId)
    } yield (t, tm))
      .filter {
        case (t, tm) =>
          tm.accountId === accountId.value.asString && t.id === threadId.value.asString
      }.map { case (_, tm) => tm }.result
    Source.fromPublisher(db.stream(q)).drop(offset.getOrElse(0)).take(limit.getOrElse(100))
  }

  def getMessagesByThreadIdSource(
      accountId: AccountId,
      threadId: ThreadId,
      offset: Option[Long],
      limit: Option[Long]
  ): Source[ThreadMessageRecord, NotUsed] = {
    val q = (for {
      (((t, tm), ta), m) <- (ThreadDao join ThreadMemberIdsDao on (_.id === _.threadId)) join ThreadAdministratorIdsDao on {
        case ((t, _), ta) =>
          t.id === ta.threadId
      } join ThreadMessageDao on {
        case (((t, _), _), m) =>
          t.id === m.threadId
      }
    } yield (t, tm, ta, m))
      .filter {
        case (t, tm, ta, m) =>
          (tm.accountId === accountId.value.asString || ta.accountId === accountId.value.asString) && t.id === threadId.value.asString
      }.map { case (_, _, _, m) => m }.result
    Source.fromPublisher(db.stream(q)).drop(offset.getOrElse(0)).take(limit.getOrElse(100))
  }

}
