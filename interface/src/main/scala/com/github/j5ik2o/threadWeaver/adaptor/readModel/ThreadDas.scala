package com.github.j5ik2o.threadWeaver.adaptor.readModel

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

  def getThreadsByMemberId(memberId: AccountId): Source[ThreadRecord, NotUsed] = {
    val q = (for {
      (t, tm) <- ThreadDao join ThreadMemberIdsDao on (_.id === _.threadId)
    } yield (t, tm))
      .filter {
        case (_, tm) =>
          tm.accountId === memberId.value.asString
      }.map { case (t, _) => t }.result
    Source.fromPublisher(db.stream(q))
  }

  def getAdministratorsByThreadId(threadId: ThreadId): Source[ThreadAdministratorIdsRecord, NotUsed] = {
    val q = ThreadAdministratorIdsDao.filter(_.threadId === threadId.value.asString).result
    Source.fromPublisher(db.stream(q))
  }

  def getMembersByThreadId(threadId: ThreadId): Source[ThreadMemberIdsRecord, NotUsed] = {
    val q = ThreadMemberIdsDao.filter(_.threadId === threadId.value.asString).result
    Source.fromPublisher(db.stream(q))
  }

  def getMessagesByThreadIdSource(threadId: ThreadId): Source[ThreadMessageRecord, NotUsed] = {
    val q = ThreadMessageDao.filter(_.threadId === threadId.value.asString).result
    Source.fromPublisher(db.stream(q))
  }

}
