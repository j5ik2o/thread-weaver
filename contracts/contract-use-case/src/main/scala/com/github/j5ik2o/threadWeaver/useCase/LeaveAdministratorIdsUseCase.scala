package com.github.j5ik2o.threadWeaver.useCase

import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  LeaveAdministratorIds,
  LeaveAdministratorIdsResponse
}

trait LeaveAdministratorIdsUseCase extends ThreadWeaverUseCase[LeaveAdministratorIds, LeaveAdministratorIdsResponse]
