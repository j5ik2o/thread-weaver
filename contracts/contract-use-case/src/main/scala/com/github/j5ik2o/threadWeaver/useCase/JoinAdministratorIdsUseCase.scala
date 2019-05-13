package com.github.j5ik2o.threadWeaver.useCase

import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{
  JoinAdministratorIds,
  JoinAdministratorIdsResponse
}

trait JoinAdministratorIdsUseCase extends ThreadWeaverUseCase[JoinAdministratorIds, JoinAdministratorIdsResponse]
