package com.github.j5ik2o.threadWeaver.useCase

import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ AddMessages, AddMessagesResponse }

trait AddMessagesUseCase extends ThreadWeaverUseCase[AddMessages, AddMessagesResponse]
