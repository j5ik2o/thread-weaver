package com.github.j5ik2o.threadWeaver.useCase

import java.time.Instant

import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
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
      title: ThreadTitle,
      remarks: Option[ThreadRemarks],
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
  // --- 管理者の削除
  final case class RemoveAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      administratorIds: AdministratorIds,
      createAt: Instant
  ) extends ThreadWeaverRequest
  sealed trait RemoveAdministratorIdsResponse extends ThreadWeaverResponse
  final case class RemoveAdministratorIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends RemoveAdministratorIdsResponse
  final case class RemoveAdministratorIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends RemoveAdministratorIdsResponse

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
  // --- メンバーの削除
  final case class RemoveMemberIds(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      memberIds: MemberIds,
      createAt: Instant
  ) extends ThreadWeaverRequest
  sealed trait RemoveMemberIdsResponse extends ThreadWeaverResponse
  final case class RemoveMemberIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends RemoveMemberIdsResponse
  final case class RemoveMemberIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends RemoveMemberIdsResponse

  // --- メッセージの追加
  final case class AddMessages(
      id: ULID,
      threadId: ThreadId,
      messages: Seq[TextMessage],
      createdAt: Instant
  ) extends ThreadWeaverRequest
  sealed trait AddMessagesResponse extends ThreadWeaverResponse
  final case class AddMessagesSucceeded(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      messageIds: MessageIds,
      createAt: Instant
  ) extends AddMessagesResponse
  final case class AddMessagesFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends AddMessagesResponse
  // --- メッセージの削除
  final case class RemoveMessages(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      messageIds: MessageIds,
      createdAt: Instant
  ) extends ThreadWeaverRequest
  sealed trait RemoveMessagesResponse extends ThreadWeaverResponse
  final case class RemoveMessagesSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends RemoveMessagesResponse
  final case class RemoveMessagesFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends RemoveMessagesResponse
}
