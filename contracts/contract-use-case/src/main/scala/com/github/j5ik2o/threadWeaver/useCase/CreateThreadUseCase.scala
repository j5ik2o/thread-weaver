package com.github.j5ik2o.threadWeaver.useCase

import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ CreateThread, CreateThreadResponse }

trait CreateThreadUseCase extends ThreadWeaverUseCase[CreateThread, CreateThreadResponse]
