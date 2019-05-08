package com.github.j5ik2o.threadWeaver.useCase

import java.time.Instant

import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ AdministratorIds, MemberIds, ThreadId }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object ThreadWeaverProtocol {

  sealed trait ThreadWeaverRequest
  sealed trait ThreadWeaverResponse

  // --- 作成
  final case class CreateThread(
      id: ULID,
      threadId: ThreadId,
      creatorId: AccountId,
      parentThreadId: Option[ThreadId],
      administratorIds: AdministratorIds,
      memberIds: MemberIds,
      createAt: Instant
  ) extends ThreadWeaverRequest

  sealed trait CreateThreadResponse extends ThreadWeaverResponse
  final case class CreateThreadSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends CreateThreadResponse
  final case class CreateThreadFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends CreateThreadResponse

  // --- 管理者の追加
  final case class AddAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      administratorIds: AdministratorIds,
      createAt: Instant
  ) extends ThreadWeaverRequest
  sealed trait AddAdministratorIdsResponse extends ThreadWeaverResponse
  final case class AddAdministratorIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends AddAdministratorIdsResponse
  final case class AddAdministratorIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends AddAdministratorIdsResponse

  // --- メンバーの追加
  final case class AddMemberIds(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      memberIds: MemberIds,
      createAt: Instant
  ) extends ThreadWeaverRequest
  sealed trait AddMemberIdsResponse extends ThreadWeaverResponse
  final case class AddMemberIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends AddMemberIdsResponse
  final case class AddMemberIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends AddMemberIdsResponse

  final case class AddMessages(id: ULID,
                               threadId: ThreadId,
                               senderId: AccountId,
                               messages: Seq[String],
                               createdAt: Instant)
      extends ThreadWeaverRequest
  sealed trait AddMessagesResponse extends ThreadWeaverResponse
  final case class AddMessagesSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends AddMessagesResponse
  final case class AddMessagesFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends AddMessagesResponse
}
