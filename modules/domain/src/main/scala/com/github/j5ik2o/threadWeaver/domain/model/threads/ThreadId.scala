package com.github.j5ik2o.threadWeaver.domain.model.threads

import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

final case class ThreadId(value: ULID = ULID())
