package com.github.j5ik2o.threadWeaver.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, MessageEntity }
import akka.stream.{ ActorMaterializer, OverflowStrategy, QueueOfferResult }
import akka.stream.scaladsl.{ Keep, Sink, Source }

import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success }

object Main extends App {
  implicit val system = ActorSystem("client")
  implicit val mat    = ActorMaterializer()
  implicit val ec     = system.dispatcher
  val host            = args(0)
  val port            = args(1).toInt

  Marshal("").to[MessageEntity]

}
