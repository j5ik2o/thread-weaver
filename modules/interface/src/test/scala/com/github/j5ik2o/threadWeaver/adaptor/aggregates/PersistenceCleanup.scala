package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.io.File

import akka.actor.ActorSystem
import org.apache.commons.io.FileUtils

import scala.util.Try

trait PersistenceCleanup {

  def storageLocations(implicit system: ActorSystem): Seq[File] =
    List(
      "akka.persistence.journal.leveldb.dir",
      "akka.persistence.journal.leveldb-shared.store.dir",
      "akka.persistence.snapshot-store.local.dir"
    ).map { s =>
      new File(system.settings.config.getString(s))
    }

  def deleteStorageLocations(implicit system: ActorSystem): Unit = {
    storageLocations.foreach(dir => Try(FileUtils.deleteDirectory(dir)))
  }

}
