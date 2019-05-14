package com.github.j5ik2o.threadWeaver.useCase

import wvlet.airframe._

object DISettings {

  def design: Design =
    newDesign
      .bind[CreateThreadUseCase].to[CreateThreadUseCaseImpl]
      .bind[DestroyThreadUseCase].to[DestroyThreadUseCaseImpl]
      .bind[JoinAdministratorIdsUseCase].to[JoinAdministratorIdsUseCaseImpl]
      .bind[LeaveAdministratorIdsUseCase].to[LeaveAdministratorIdsUseCaseImpl]
      .bind[JoinMemberIdsUseCase].to[JoinMemberIdsUseCaseImpl]
      .bind[LeaveMemberIdsUseCase].to[LeaveMemberIdsUseCaseImpl]
      .bind[AddMessagesUseCase].to[AddMessagesUseCaseImpl]
      .bind[RemoveMessagesUseCase].to[RemoveMessagesUseCaseImpl]

}
