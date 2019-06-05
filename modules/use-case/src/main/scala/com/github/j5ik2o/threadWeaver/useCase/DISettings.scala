package com.github.j5ik2o.threadWeaver.useCase

import akka.actor.typed.ActorSystem
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.typed.ThreadProtocol.ThreadActorRefOfCommandTypeRef
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol.ThreadActorRefOfCommandUntypeRef
import com.github.j5ik2o.threadWeaver.useCase.typed._
import com.github.j5ik2o.threadWeaver.useCase.untyped._
import wvlet.airframe._
import akka.actor.typed.scaladsl.adapter._

import scala.concurrent.duration._

object DISettings {

  // def design(timeout: FiniteDuration): Design = designOfTyped(timeout)

  def design(timeout: FiniteDuration): Design = designOfUntyped(timeout)

  def designOfUntyped(timeout: FiniteDuration): Design =
    newDesign
      .bind[CreateThreadUseCase].toProvider[ThreadActorRefOfCommandUntypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem.toUntyped
          new CreateThreadUseCaseUntypeImpl(ref, 1, timeout)
      }
      .bind[DestroyThreadUseCase].toProvider[ThreadActorRefOfCommandUntypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem.toUntyped
          new DestroyThreadUseCaseUntypeImpl(ref, 1, timeout)
      }
      .bind[JoinAdministratorIdsUseCase].toProvider[ThreadActorRefOfCommandUntypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem.toUntyped
          new JoinAdministratorIdsUseCaseUntypeImpl(ref, 1, timeout)
      }
      .bind[LeaveAdministratorIdsUseCase].toProvider[ThreadActorRefOfCommandUntypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem.toUntyped
          new LeaveAdministratorIdsUseCaseUntypeImpl(ref, 1, timeout)
      }
      .bind[JoinMemberIdsUseCase].toProvider[ThreadActorRefOfCommandUntypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem.toUntyped
          new JoinMemberIdsUseCaseUntypeImpl(ref, 1, timeout)
      }
      .bind[LeaveMemberIdsUseCase].toProvider[ThreadActorRefOfCommandUntypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem.toUntyped
          new LeaveMemberIdsUseCaseUntypeImpl(ref, 1, timeout)
      }
      .bind[AddMessagesUseCase].toProvider[ThreadActorRefOfCommandUntypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem.toUntyped
          new AddMessagesUseCaseUntypeImpl(ref, 1, timeout)
      }
      .bind[RemoveMessagesUseCase].toProvider[ThreadActorRefOfCommandUntypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem.toUntyped
          new RemoveMessagesUseCaseUntypeImpl(ref, 1, timeout)
      }

  def designOfTyped(timeout: FiniteDuration): Design =
    newDesign
      .bind[CreateThreadUseCase].toProvider[ThreadActorRefOfCommandTypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new CreateThreadUseCaseTypeImpl(ref, 1, timeout)
      }
      .bind[DestroyThreadUseCase].toProvider[ThreadActorRefOfCommandTypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new DestroyThreadUseCaseTypeImpl(ref, 1, timeout)
      }
      .bind[JoinAdministratorIdsUseCase].toProvider[ThreadActorRefOfCommandTypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new JoinAdministratorIdsUseCaseTypeImpl(ref, 1, timeout)
      }
      .bind[LeaveAdministratorIdsUseCase].toProvider[ThreadActorRefOfCommandTypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new LeaveAdministratorIdsUseCaseTypeImpl(ref, 1, timeout)
      }
      .bind[JoinMemberIdsUseCase].toProvider[ThreadActorRefOfCommandTypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new JoinMemberIdsUseCaseTypeImpl(ref, 1, timeout)
      }
      .bind[LeaveMemberIdsUseCase].toProvider[ThreadActorRefOfCommandTypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new LeaveMemberIdsUseCaseTypeImpl(ref, 1, timeout)
      }
      .bind[AddMessagesUseCase].toProvider[ThreadActorRefOfCommandTypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new AddMessagesUseCaseTypeImpl(ref, 1, timeout)
      }
      .bind[RemoveMessagesUseCase].toProvider[ThreadActorRefOfCommandTypeRef, ActorSystem[Nothing]] {
        case (ref, actorSystem) =>
          implicit val ac = actorSystem
          new RemoveMessagesUseCaseTypeImpl(ref, 1, timeout)
      }
}
