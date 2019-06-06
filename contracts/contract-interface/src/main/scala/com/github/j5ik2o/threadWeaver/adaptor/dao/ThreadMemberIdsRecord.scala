package com.github.j5ik2o.threadWeaver.adaptor.dao

trait ThreadMemberIdsRecord {
  def id: String
  def threadId: String
  def accountId: String
  def adderId: String
  def createdAt: java.time.Instant
  def updatedAt: java.time.Instant
}
