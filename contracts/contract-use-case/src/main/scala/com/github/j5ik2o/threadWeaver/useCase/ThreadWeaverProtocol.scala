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
  final case class JoinAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      administratorIds: AdministratorIds,
      createAt: Instant
  ) extends ThreadWeaverRequest
  sealed trait JoinAdministratorIdsResponse extends ThreadWeaverResponse
  final case class JoinAdministratorIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends JoinAdministratorIdsResponse
  final case class JoinAdministratorIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends JoinAdministratorIdsResponse
  // --- 管理者の削除
  final case class LeaveAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      administratorIds: AdministratorIds,
      createAt: Instant
  ) extends ThreadWeaverRequest
  sealed trait LeaveAdministratorIdsResponse extends ThreadWeaverResponse
  final case class LeaveAdministratorIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends LeaveAdministratorIdsResponse
  final case class LeaveAdministratorIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends LeaveAdministratorIdsResponse

  // --- メンバーの追加
  final case class JoinMemberIds(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      memberIds: MemberIds,
      createAt: Instant
  ) extends ThreadWeaverRequest
  sealed trait JoinMemberIdsResponse extends ThreadWeaverResponse
  final case class JoinMemberIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends JoinMemberIdsResponse
  final case class JoinMemberIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends JoinMemberIdsResponse
  // --- メンバーの削除
  final case class LeaveMemberIds(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      memberIds: MemberIds,
      createAt: Instant
  ) extends ThreadWeaverRequest
  sealed trait LeaveMemberIdsResponse extends ThreadWeaverResponse
  final case class LeaveMemberIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends LeaveMemberIdsResponse
  final case class LeaveMemberIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends LeaveMemberIdsResponse

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
  final case class RemoveMessagesSucceeded(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      messageIds: MessageIds,
      createAt: Instant
  ) extends RemoveMessagesResponse
  final case class RemoveMessagesFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends RemoveMessagesResponse
}
