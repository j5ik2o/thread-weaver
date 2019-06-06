package com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc

import slick.lifted.ProvenShape
import slick.lifted.PrimaryKey
import com.github.j5ik2o.threadWeaver.adaptor.dao._

trait ThreadMemberIdsComponent extends SlickDaoSupport {

  import profile.api._

  case class ThreadMemberIdsRecordImpl(
      id: String,
      threadId: String,
      accountId: String,
      adderId: String,
      createdAt: java.time.Instant,
      updatedAt: java.time.Instant
  ) extends Record
      with ThreadMemberIdsRecord

  case class ThreadMemberIdss(tag: Tag) extends TableBase[ThreadMemberIdsRecordImpl](tag, "thread_member_ids") {
    def id: Rep[String]                   = column[String]("id")
    def threadId: Rep[String]             = column[String]("thread_id")
    def accountId: Rep[String]            = column[String]("account_id")
    def adderId: Rep[String]              = column[String]("adder_id")
    def createdAt: Rep[java.time.Instant] = column[java.time.Instant]("created_at")
    def updatedAt: Rep[java.time.Instant] = column[java.time.Instant]("updated_at")
    def pk: PrimaryKey                    = primaryKey("pk", (id))
    override def * : ProvenShape[ThreadMemberIdsRecordImpl] =
      (id, threadId, accountId, adderId, createdAt, updatedAt) <> (ThreadMemberIdsRecordImpl.tupled, ThreadMemberIdsRecordImpl.unapply)
  }

  object ThreadMemberIdsDao extends TableQuery(ThreadMemberIdss)

}
