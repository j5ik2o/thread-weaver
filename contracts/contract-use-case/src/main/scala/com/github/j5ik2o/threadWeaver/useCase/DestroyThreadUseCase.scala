package com.github.j5ik2o.threadWeaver.useCase

import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ DestroyThread, DestroyThreadResponse }

trait DestroyThreadUseCase extends ThreadWeaverUseCase[DestroyThread, DestroyThreadResponse]
