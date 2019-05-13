package com.github.j5ik2o.threadWeaver.useCase

import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ LeaveMemberIds, LeaveMemberIdsResponse }

trait LeaveMemberIdsUseCase extends ThreadWeaverUseCase[LeaveMemberIds, LeaveMemberIdsResponse]
