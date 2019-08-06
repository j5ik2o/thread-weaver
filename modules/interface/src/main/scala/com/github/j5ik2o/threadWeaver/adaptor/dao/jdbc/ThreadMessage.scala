package com.github.j5ik2o.threadWeaver.adaptor.dao.jdbc

import slick.lifted.ProvenShape
import slick.lifted.PrimaryKey
import com.github.j5ik2o.threadWeaver.adaptor.dao._

trait ThreadMessageComponent extends SlickDaoSupport {

import profile.api._

case class ThreadMessageRecordImpl(
    id: String,
        deleted: Boolean,
        threadId: String,
        senderId: String,
        `type`: String,
        body: String,
        createdAt: java.time.Instant,
        updatedAt: java.time.Instant
) extends SoftDeletableRecord with ThreadMessageRecord

case class ThreadMessages(tag: Tag) extends TableBase[ThreadMessageRecordImpl](tag, "thread_message") with SoftDeletableTableSupport[ThreadMessageRecordImpl] {
    def id: Rep[String] = column[String]("id")
        def deleted: Rep[Boolean] = column[Boolean]("deleted")
        def threadId: Rep[String] = column[String]("thread_id")
        def senderId: Rep[String] = column[String]("sender_id")
        def `type`: Rep[String] = column[String]("type")
        def body: Rep[String] = column[String]("body")
        def createdAt: Rep[java.time.Instant] = column[java.time.Instant]("created_at")
        def updatedAt: Rep[java.time.Instant] = column[java.time.Instant]("updated_at")
def pk: PrimaryKey  = primaryKey("pk", (id))
override def * : ProvenShape[ThreadMessageRecordImpl] = (id,deleted,threadId,senderId,`type`,body,createdAt,updatedAt) <> (ThreadMessageRecordImpl.tupled, ThreadMessageRecordImpl.unapply)
}

object ThreadMessageDao extends TableQuery(ThreadMessages)

}
