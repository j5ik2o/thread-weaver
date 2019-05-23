package com.github.j5ik2o.threadWeaver.useCase

import akka.actor.typed.ActorSystem
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol.ThreadActorRefOfCommand
import wvlet.airframe._
import scala.concurrent.duration._

object DISettings {

  def design(timeout: FiniteDuration): Design =
    newDesign
      .bind[CreateThreadUseCase].toProvider[ThreadActorRefOfCommand, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new CreateThreadUseCaseImpl(ref, 1, timeout)
      }
      .bind[DestroyThreadUseCase].toProvider[ThreadActorRefOfCommand, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new DestroyThreadUseCaseImpl(ref, 1, timeout)
      }
      .bind[JoinAdministratorIdsUseCase].toProvider[ThreadActorRefOfCommand, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new JoinAdministratorIdsUseCaseImpl(ref, 1, timeout)
      }
      .bind[LeaveAdministratorIdsUseCase].toProvider[ThreadActorRefOfCommand, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new LeaveAdministratorIdsUseCaseImpl(ref, 1, timeout)
      }
      .bind[JoinMemberIdsUseCase].toProvider[ThreadActorRefOfCommand, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new JoinMemberIdsUseCaseImpl(ref, 1, timeout)
      }
      .bind[LeaveMemberIdsUseCase].toProvider[ThreadActorRefOfCommand, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new LeaveMemberIdsUseCaseImpl(ref, 1, timeout)
      }
      .bind[AddMessagesUseCase].toProvider[ThreadActorRefOfCommand, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new AddMessagesUseCaseImpl(ref, 1, timeout)
      }
      .bind[RemoveMessagesUseCase].toProvider[ThreadActorRefOfCommand, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new RemoveMessagesUseCaseImpl(ref, 1, timeout)
      }

}
