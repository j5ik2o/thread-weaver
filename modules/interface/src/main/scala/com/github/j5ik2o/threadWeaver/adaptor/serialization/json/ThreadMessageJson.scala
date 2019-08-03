package com.github.j5ik2o.threadWeaver.adaptor.serialization.json
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.CreateThread
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso

case class CreateThreadJson(
    id: String,
    threadId: String,
    creatorId: String,
    parentThreadId: Option[String],
    title: String,
    remarks: Option[String],
    administratorIds: Seq[String],
    memberIds: Seq[String],
    createAt: Long,
    reply: Boolean
)

object CreateThreadJson {

  implicit object Iso extends DomainObjToJsonReprIso[CreateThread, CreateThreadJson] {
    override def convertTo(domainObj: CreateThread): CreateThreadJson = ???

    override def convertFrom(json: CreateThreadJson): CreateThread = ???
  }

}
