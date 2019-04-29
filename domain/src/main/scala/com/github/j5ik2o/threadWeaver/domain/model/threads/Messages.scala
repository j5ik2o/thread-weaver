package com.github.j5ik2o.threadWeaver.domain.model.threads

import cats.Monoid
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId

final case class Messages(breachEncapsulationOfValues: Vector[Message[_]]) {

  def filterNot(messageIds: MessageIds, senderId: AccountId): Messages = {
    copy(
      breachEncapsulationOfValues =
        breachEncapsulationOfValues.filterNot(v => v.senderId == senderId && messageIds.contains(v.id))
    )
  }

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
