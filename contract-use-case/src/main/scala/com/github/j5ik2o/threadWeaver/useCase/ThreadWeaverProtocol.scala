package com.github.j5ik2o.threadWeaver.useCase

import java.time.Instant

import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ AdministratorIds, MemberIds, ThreadId }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object ThreadWeaverProtocol {

  sealed trait ThreadWeaverRequest
  sealed trait ThreadWeaverResponse

  case class CreateThread(
      id: ULID,
      threadId: ThreadId,
      creatorId: AccountId,
      parentThreadId: Option[ThreadId],
      administratorIds: AdministratorIds,
      memberIds: MemberIds,
      createAt: Instant
  ) extends ThreadWeaverRequest

  sealed trait CreateThreadResponse extends ThreadWeaverResponse
  case class CreateThreadSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends CreateThreadResponse
  case class CreateThreadFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends CreateThreadResponse

}
