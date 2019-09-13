package com.github.j5ik2o.threadWeaver.adaptor.serialization

import akka.actor.ExtendedActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.serialization.SerializerWithStringManifest
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.adaptor.serialization.json._

object ThreadMessageJSONSerializer {
  val CREATE_THREAD: String           = ThreadProtocol.CreateThread.getClass.getName.stripSuffix("$")
  val CREATE_THREAD_SUCCEEDED: String = ThreadProtocol.CreateThreadSucceeded.getClass.getName.stripSuffix("$")
  val CREATE_THREAD_FAILED: String    = ThreadProtocol.CreateThreadFailed.getClass.getName.stripSuffix("$")

  val EXISTS_THREAD: String           = ThreadProtocol.ExistsThread.getClass.getName.stripSuffix("$")
  val EXISTS_THREAD_SUCCEEDED: String = ThreadProtocol.ExistsThreadSucceeded.getClass.getName.stripSuffix("$")
  val EXISTS_THREAD_FAILED: String    = ThreadProtocol.ExistsThreadFailed.getClass.getName.stripSuffix("$")

  val DESTROY_THREAD: String           = ThreadProtocol.DestroyThread.getClass.getName.stripSuffix("$")
  val DESTROY_THREAD_SUCCEEDED: String = ThreadProtocol.DestroyThreadSucceeded.getClass.getName.stripSuffix("$")
  val DESTROY_THREAD_FAILED: String    = ThreadProtocol.DestroyThreadFailed.getClass.getName.stripSuffix("$")

  val ADD_MESSAGES: String           = ThreadProtocol.AddMessages.getClass.getName.stripSuffix("$")
  val ADD_MESSAGES_SUCCEEDED: String = ThreadProtocol.AddMessagesSucceeded.getClass.getName.stripSuffix("$")
  val ADD_MESSAGES_FAILED: String    = ThreadProtocol.AddMessagesFailed.getClass.getName.stripSuffix("$")
}

class ThreadMessageJSONSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.AddMessagesFailedJson._
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.AddMessagesJson._
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.AddMessagesSucceededJson._
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.CreateThreadFailedJson._
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.CreateThreadJson._
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.CreateThreadSucceededJson._
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.DestroyThreadFailedJson._
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.DestroyThreadJson._
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.DestroyThreadSucceededJson._
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.ExistsThreadFailedJson._
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.ExistsThreadJson._
  import com.github.j5ik2o.threadWeaver.adaptor.serialization.json.ExistsThreadSucceededJson._
  import io.circe.generic.auto._
  private implicit val log: LoggingAdapter = Logging.getLogger(system, getClass)

  override def identifier: Int = 51

  override def manifest(o: AnyRef): String = o.getClass.getName

  // scalastyle:off
  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case orig: ThreadProtocol.CreateThread          => CirceJsonSerialization.toBinary(orig)
    case orig: ThreadProtocol.CreateThreadSucceeded => CirceJsonSerialization.toBinary(orig)
    case orig: ThreadProtocol.CreateThreadFailed    => CirceJsonSerialization.toBinary(orig)

    case orig: ThreadProtocol.ExistsThread          => CirceJsonSerialization.toBinary(orig)
    case orig: ThreadProtocol.ExistsThreadSucceeded => CirceJsonSerialization.toBinary(orig)
    case orig: ThreadProtocol.ExistsThreadFailed    => CirceJsonSerialization.toBinary(orig)

    case orig: ThreadProtocol.DestroyThread          => CirceJsonSerialization.toBinary(orig)
    case orig: ThreadProtocol.DestroyThreadSucceeded => CirceJsonSerialization.toBinary(orig)
    case orig: ThreadProtocol.DestroyThreadFailed    => CirceJsonSerialization.toBinary(orig)

    case orig: ThreadProtocol.AddMessages          => CirceJsonSerialization.toBinary(orig)
    case orig: ThreadProtocol.AddMessagesSucceeded => CirceJsonSerialization.toBinary(orig)
    case orig: ThreadProtocol.AddMessagesFailed    => CirceJsonSerialization.toBinary(orig)
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case ThreadMessageJSONSerializer.CREATE_THREAD =>
      CirceJsonSerialization.fromBinary[CreateThread, CreateThreadJson](bytes)
    case ThreadMessageJSONSerializer.CREATE_THREAD_SUCCEEDED =>
      CirceJsonSerialization.fromBinary[CreateThreadSucceeded, CreateThreadSucceededJson](bytes)
    case ThreadMessageJSONSerializer.CREATE_THREAD_FAILED =>
      CirceJsonSerialization.fromBinary[CreateThreadFailed, CreateThreadFailedJson](bytes)

    case ThreadMessageJSONSerializer.EXISTS_THREAD =>
      CirceJsonSerialization.fromBinary[ExistsThread, ExistsThreadJson](bytes)
    case ThreadMessageJSONSerializer.EXISTS_THREAD_SUCCEEDED =>
      CirceJsonSerialization.fromBinary[ExistsThreadSucceeded, ExistsThreadSucceededJson](bytes)
    case ThreadMessageJSONSerializer.EXISTS_THREAD_FAILED =>
      CirceJsonSerialization.fromBinary[ExistsThreadFailed, ExistsThreadFailedJson](bytes)

    case ThreadMessageJSONSerializer.DESTROY_THREAD =>
      CirceJsonSerialization.fromBinary[DestroyThread, DestroyThreadJson](bytes)
    case ThreadMessageJSONSerializer.DESTROY_THREAD_SUCCEEDED =>
      CirceJsonSerialization.fromBinary[DestroyThreadSucceeded, DestroyThreadSucceededJson](bytes)
    case ThreadMessageJSONSerializer.DESTROY_THREAD_FAILED =>
      CirceJsonSerialization.fromBinary[DestroyThreadFailed, DestroyThreadFailedJson](bytes)

    case ThreadMessageJSONSerializer.ADD_MESSAGES =>
      CirceJsonSerialization.fromBinary[AddMessages, AddMessagesJson](bytes)
    case ThreadMessageJSONSerializer.ADD_MESSAGES_SUCCEEDED =>
      CirceJsonSerialization.fromBinary[AddMessagesSucceeded, AddMessagesSucceededJson](bytes)
    case ThreadMessageJSONSerializer.ADD_MESSAGES_FAILED =>
      CirceJsonSerialization.fromBinary[AddMessagesFailed, AddMessagesFailedJson](bytes)
  }
  // scalastyle:on
}
