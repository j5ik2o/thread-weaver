package com.github.j5ik2o.threadWeaver.adaptor.presenter

import akka.NotUsed
import akka.stream.scaladsl.Flow

trait Presenter[Res, ResRepr] {
  def response: Flow[Res, ResRepr, NotUsed]
}
