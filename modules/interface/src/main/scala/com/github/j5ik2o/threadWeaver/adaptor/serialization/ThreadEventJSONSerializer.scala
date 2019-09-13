package com.github.j5ik2o.threadWeaver.adaptor.serialization

import akka.actor.ExtendedActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.serialization.SerializerWithStringManifest
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.ThreadCreated
import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.ThreadCreatedJson

object ThreadEventJSONSerializer {
  final val CREATE = ThreadProtocol.ThreadCreated.getClass.getName.stripSuffix("$")
}

class ThreadEventJSONSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.ThreadCreatedJson._
  import io.circe.generic.auto._
  private implicit val log: LoggingAdapter = Logging.getLogger(system, getClass)

  override def identifier: Int = 50

  override def manifest(o: AnyRef): String = o.getClass.getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case orig: ThreadCreated => CirceJsonSerialization.toBinary(orig)
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case ThreadEventJSONSerializer.CREATE => CirceJsonSerialization.fromBinary[ThreadCreated, ThreadCreatedJson](bytes)
  }
}
