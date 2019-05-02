package com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc

trait ThreadAdministratorIdsComponent extends SlickDaoSupport {

  import profile.api._

  case class ThreadAdministratorIdsRecord(
      id: String,
      threadId: String,
      accountId: String,
      adderId: String,
      createdAt: java.time.Instant,
      updatedAt: java.time.Instant
  ) extends Record

  case class ThreadAdministratorIdss(tag: Tag)
      extends TableBase[ThreadAdministratorIdsRecord](tag, "thread_administrator_ids") {
    def id: Rep[String]                   = column[String]("id")
    def threadId: Rep[String]             = column[String]("thread_id")
    def accountId: Rep[String]            = column[String]("account_id")
    def adderId: Rep[String]              = column[String]("adder_id")
    def createdAt: Rep[java.time.Instant] = column[java.time.Instant]("created_at")
    def updatedAt: Rep[java.time.Instant] = column[java.time.Instant]("updated_at")
    def pk                                = primaryKey("pk", (id))
    override def * =
      (id, threadId, accountId, adderId, createdAt, updatedAt) <> (ThreadAdministratorIdsRecord.tupled, ThreadAdministratorIdsRecord.unapply)
  }

  object ThreadAdministratorIdsDao extends TableQuery(ThreadAdministratorIdss)

}
