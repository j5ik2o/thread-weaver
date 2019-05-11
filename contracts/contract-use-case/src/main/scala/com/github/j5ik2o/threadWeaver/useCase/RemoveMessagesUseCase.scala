package com.github.j5ik2o.threadWeaver.useCase

import com.github.j5ik2o.threadWeaver.useCase.ThreadWeaverProtocol.{ RemoveMessages, RemoveMessagesResponse }

trait RemoveMessagesUseCase extends ThreadWeaverUseCase[RemoveMessages, RemoveMessagesResponse]
