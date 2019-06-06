package com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc

import slick.lifted.ProvenShape
import slick.lifted.PrimaryKey
import com.github.j5ik2o.threadWeaver.adaptor.dao._

trait ThreadComponent extends SlickDaoSupport {

  import profile.api._

  case class ThreadRecordImpl(
      id: String,
      deleted: Boolean,
      sequenceNr: Long,
      creatorId: String,
      parentId: Option[String],
      title: String,
      remarks: Option[String],
      createdAt: java.time.Instant,
      updatedAt: java.time.Instant,
      removedAt: Option[java.time.Instant]
  ) extends SoftDeletableRecord
      with ThreadRecord

  case class Threads(tag: Tag)
      extends TableBase[ThreadRecordImpl](tag, "thread")
      with SoftDeletableTableSupport[ThreadRecordImpl] {
    def id: Rep[String]                           = column[String]("id")
    def deleted: Rep[Boolean]                     = column[Boolean]("deleted")
    def sequenceNr: Rep[Long]                     = column[Long]("sequence_nr")
    def creatorId: Rep[String]                    = column[String]("creator_id")
    def parentId: Rep[Option[String]]             = column[Option[String]]("parent_id")
    def title: Rep[String]                        = column[String]("title")
    def remarks: Rep[Option[String]]              = column[Option[String]]("remarks")
    def createdAt: Rep[java.time.Instant]         = column[java.time.Instant]("created_at")
    def updatedAt: Rep[java.time.Instant]         = column[java.time.Instant]("updated_at")
    def removedAt: Rep[Option[java.time.Instant]] = column[Option[java.time.Instant]]("removed_at")
    def pk: PrimaryKey                            = primaryKey("pk", (id))
    override def * : ProvenShape[ThreadRecordImpl] =
      (id, deleted, sequenceNr, creatorId, parentId, title, remarks, createdAt, updatedAt, removedAt) <> (ThreadRecordImpl.tupled, ThreadRecordImpl.unapply)
  }

  object ThreadDao extends TableQuery(Threads)

}
