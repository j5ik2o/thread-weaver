package com.github.j5ik2o.threadWeaver.domain.model.threads

final case class Text(value: String) {
  override def toString: String = value
}

object Text {

  def parseFrom(text: String): Either[Exception, Text] =
    if (text.size > 255)
      Left(new IllegalArgumentException("too long text"))
    else
      Right(Text(text))
}
