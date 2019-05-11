package com.github.j5ik2o.threadWeaver.domain.model.threads

import cats.Monoid

final case class MessageIds(breachEncapsulationOfValues: Vector[MessageId]) {

  def contains(value: MessageId): Boolean =
    breachEncapsulationOfValues.contains(value)

  def valuesAsString: Vector[String] = breachEncapsulationOfValues.map(_.value.asString)

}

object MessageIds {

  def apply(values: MessageId*): MessageIds = new MessageIds(Vector(values: _*))

  val empty = MessageIds(Vector.empty)

  implicit val memberIdsMonoid: Monoid[MessageIds] = new Monoid[MessageIds] {
    override def empty: MessageIds = MessageIds.empty

    override def combine(x: MessageIds, y: MessageIds): MessageIds =
      MessageIds(x.breachEncapsulationOfValues ++ y.breachEncapsulationOfValues)
  }

}
