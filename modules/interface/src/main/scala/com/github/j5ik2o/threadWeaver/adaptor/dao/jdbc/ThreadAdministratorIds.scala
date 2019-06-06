package com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc

import slick.lifted.ProvenShape
import slick.lifted.PrimaryKey
import com.github.j5ik2o.threadWeaver.adaptor.dao._

trait ThreadAdministratorIdsComponent extends SlickDaoSupport {

  import profile.api._

  case class ThreadAdministratorIdsRecordImpl(
      id: String,
      threadId: String,
      accountId: String,
      adderId: String,
      createdAt: java.time.Instant,
      updatedAt: java.time.Instant
  ) extends Record
      with ThreadAdministratorIdsRecord

  case class ThreadAdministratorIdss(tag: Tag)
      extends TableBase[ThreadAdministratorIdsRecordImpl](tag, "thread_administrator_ids") {
    def id: Rep[String]                   = column[String]("id")
    def threadId: Rep[String]             = column[String]("thread_id")
    def accountId: Rep[String]            = column[String]("account_id")
    def adderId: Rep[String]              = column[String]("adder_id")
    def createdAt: Rep[java.time.Instant] = column[java.time.Instant]("created_at")
    def updatedAt: Rep[java.time.Instant] = column[java.time.Instant]("updated_at")
    def pk: PrimaryKey                    = primaryKey("pk", (id))
    override def * : ProvenShape[ThreadAdministratorIdsRecordImpl] =
      (id, threadId, accountId, adderId, createdAt, updatedAt) <> (ThreadAdministratorIdsRecordImpl.tupled, ThreadAdministratorIdsRecordImpl.unapply)
  }

  object ThreadAdministratorIdsDao extends TableQuery(ThreadAdministratorIdss)

}
