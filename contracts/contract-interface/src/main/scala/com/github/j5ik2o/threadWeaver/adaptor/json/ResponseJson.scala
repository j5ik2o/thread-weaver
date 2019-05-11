package com.github.j5ik2o.threadWeaver.adaptor.json

trait ResponseJson {
  def error_messages: Seq[String]
  def isSuccessful: Boolean = error_messages.isEmpty
}
