package com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc

trait ThreadComponent extends SlickDaoSupport {

  import profile.api._

  case class ThreadRecord(
      id: String,
      deleted: Boolean,
      sequenceNr: Long,
      creatorId: String,
      parentId: Option[String],
      createdAt: java.time.Instant,
      updatedAt: java.time.Instant,
      removedAt: Option[java.time.Instant]
  ) extends SoftDeletableRecord

  case class Threads(tag: Tag)
      extends TableBase[ThreadRecord](tag, "thread")
      with SoftDeletableTableSupport[ThreadRecord] {
    def id: Rep[String]                           = column[String]("id")
    def deleted: Rep[Boolean]                     = column[Boolean]("deleted")
    def sequenceNr: Rep[Long]                     = column[Long]("sequence_nr")
    def creatorId: Rep[String]                    = column[String]("creator_id")
    def parentId: Rep[Option[String]]             = column[Option[String]]("parent_id")
    def createdAt: Rep[java.time.Instant]         = column[java.time.Instant]("created_at")
    def updatedAt: Rep[java.time.Instant]         = column[java.time.Instant]("updated_at")
    def removedAt: Rep[Option[java.time.Instant]] = column[Option[java.time.Instant]]("removed_at")
    def pk                                        = primaryKey("pk", (id))
    override def * =
      (id, deleted, sequenceNr, creatorId, parentId, createdAt, updatedAt, removedAt) <> (ThreadRecord.tupled, ThreadRecord.unapply)
  }

  object ThreadDao extends TableQuery(Threads)

}
