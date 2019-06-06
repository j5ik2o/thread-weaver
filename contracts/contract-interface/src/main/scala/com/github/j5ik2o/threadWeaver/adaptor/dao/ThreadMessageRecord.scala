package com.github.j5ik2o.threadWeaver.adaptor.dao

trait ThreadMessageRecord {
  def id: String
  def deleted: Boolean
  def threadId: String
  def senderId: String
  def `type`: String
  def body: String
  def createdAt: java.time.Instant
  def updatedAt: java.time.Instant
}
