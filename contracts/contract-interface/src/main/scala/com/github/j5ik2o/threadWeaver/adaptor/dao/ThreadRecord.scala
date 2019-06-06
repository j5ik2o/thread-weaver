package com.github.j5ik2o.threadWeaver.adaptor.dao

trait ThreadRecord {
  def id: String
  def deleted: Boolean
  def sequenceNr: Long
  def creatorId: String
  def parentId: Option[String]
  def title: String
  def remarks: Option[String]
  def createdAt: java.time.Instant
  def updatedAt: java.time.Instant
  def removedAt: Option[java.time.Instant]
}
