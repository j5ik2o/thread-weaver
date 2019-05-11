package com.github.j5ik2o.threadWeaver.adaptor.http.json

trait ResponseJson {
  def error_messages: Seq[String]
  def isSuccessful: Boolean = error_messages.isEmpty
}
