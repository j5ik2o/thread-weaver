package com.github.j5ik2o.threadWeaver.domain.model.threads

import cats.Monoid

final case class Messages(breachEncapsulationOfValues: Vector[Message[_]]) {

  def filter(messageIds: MessageIds): Messages =
    copy(
      breachEncapsulationOfValues = breachEncapsulationOfValues.filter(v => messageIds.contains(v.id))
    )

  def forall(condition: Message[_] => Boolean): Boolean =
    breachEncapsulationOfValues.forall(condition)

  def filterNot(messageIds: MessageIds): Messages = {
    copy(
      breachEncapsulationOfValues = breachEncapsulationOfValues.filterNot(v => messageIds.contains(v.id))
    )
  }

  def toMessageIds: MessageIds = MessageIds(breachEncapsulationOfValues.map(_.id): _*)

}

object Messages {

  def apply(values: Message[_]*): Messages = new Messages(Vector(values: _*))

  val empty = Messages(Vector.empty)

  implicit val messagesMonoid: Monoid[Messages] = new Monoid[Messages] {
    override def empty: Messages = Messages.empty

    override def combine(x: Messages, y: Messages): Messages =
      Messages(x.breachEncapsulationOfValues ++ y.breachEncapsulationOfValues)
  }
}
