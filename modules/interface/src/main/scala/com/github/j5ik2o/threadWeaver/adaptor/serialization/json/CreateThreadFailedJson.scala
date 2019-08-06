package com.github.j5ik2o.threadWeaver.adaptor.serialization.json

import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.CreateThreadFailed
import com.github.j5ik2o.threadWeaver.adaptor.serialization.DomainObjToJsonReprIso

case class CreateThreadFailedJson(id: String, requestId: String, threadId: String, message: String, createAt: Long)

object CreateThreadFailedJson {
  implicit object CreateThreadFailedIso extends DomainObjToJsonReprIso[CreateThreadFailed, CreateThreadFailedJson] {
    override def convertTo(domainObj: CreateThreadFailed): CreateThreadFailedJson = ???

    override def convertFrom(json: CreateThreadFailedJson): CreateThreadFailed = ???
  }
}
