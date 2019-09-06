package com.github.j5ik2o.threadWeaver.adaptor.serialization

import java.nio.charset.StandardCharsets

import akka.event.LoggingAdapter
import io.circe._
import io.circe.parser._
import io.circe.syntax._

object StringToByteConversion {

  implicit class StringToByte(text: String) {
    def toUTF8Byte: Array[Byte] = text.getBytes(StandardCharsets.UTF_8)
  }

}

trait DomainObjToJsonReprIso[DomainObj, JsonRepr] {
  def convertTo(domainObj: DomainObj): JsonRepr
  def convertFrom(json: JsonRepr): DomainObj
}

class CirceDeserializationException(message: String, cause: Throwable) extends Exception(message, cause)

object CirceJsonSerialization {
  import StringToByteConversion._

  def toBinary[DomainObj, JsonRepr](
      orig: DomainObj
  )(
      implicit iso: DomainObjToJsonReprIso[DomainObj, JsonRepr],
      encoder: Encoder[JsonRepr],
      log: LoggingAdapter
  ): Array[Byte] = {
    val domain     = iso.convertTo(orig)
    val jsonString = domain.asJson.noSpaces
    if (log.isDebugEnabled)
      log.debug(s"toBinary: jsonString = $jsonString")
    jsonString.toUTF8Byte
  }

  def fromBinary[DomainObj, JsonRepr](
      bytes: Array[Byte]
  )(implicit iso: DomainObjToJsonReprIso[DomainObj, JsonRepr], d: Decoder[JsonRepr], log: LoggingAdapter): DomainObj = {
    val jsonString = new String(bytes, StandardCharsets.UTF_8)
    if (log.isDebugEnabled)
      log.debug(s"fromBinary: jsonString = $jsonString")
    val result = for {
      json       <- parse(jsonString).right
      resultJson <- json.as[JsonRepr].right
    } yield iso.convertFrom(resultJson)
    result match {
      case Left(failure)    => throw new CirceDeserializationException(failure.getMessage, failure)
      case Right(domainObj) => domainObj
    }
  }

}
