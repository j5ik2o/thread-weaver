package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import java.time.Instant

import akka.actor.ActorRef
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.{ BaseCommandRequest, ThreadCommonProtocol }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadCommonProtocol.Event
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object ThreadProtocol {

  type ThreadReadModelUpdaterUntypeRef  = ActorRef
  type ThreadActorRefOfMessageUntypeRef = ActorRef
  type ThreadActorRefOfCommandUntypeRef = ActorRef

  trait ToCommandRequest { this: Event =>
    def toCommandRequest: CommandRequest
  }
  sealed trait CommandMessage extends ThreadCommonProtocol.Message {
    def id: ULID
    def threadId: ThreadId
    def createAt: Instant
  }
  sealed trait CommandRequest extends CommandMessage with BaseCommandRequest {
    def senderId: AccountId
  }

  trait ToEvent { this: CommandRequest =>
    def toEvent: Event
  }
  sealed trait CommandResponse extends CommandMessage {
    def requestId: ULID
  }
  trait CommandSuccessResponse extends CommandResponse
  trait CommandFailureResponse extends CommandResponse

  // --- スレッドの生成
  final case class CreateThread(
      id: ULID,
      threadId: ThreadId,
      creatorId: AccountId,
      parentThreadId: Option[ThreadId],
      title: ThreadTitle,
      remarks: Option[ThreadRemarks],
      administratorIds: AdministratorIds,
      memberIds: MemberIds,
      createAt: Instant,
      reply: Boolean
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = creatorId
    override def toEvent: Event =
      ThreadCreated(ULID(), threadId, creatorId, parentThreadId, title, remarks, administratorIds, memberIds, createAt)
  }
  sealed trait CreateThreadResponse extends CommandResponse
  final case class CreateThreadSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends CreateThreadResponse
      with CommandSuccessResponse
  final case class CreateThreadFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends CreateThreadResponse
      with CommandFailureResponse
  final case class ThreadCreated(
      id: ULID,
      threadId: ThreadId,
      creatorId: AccountId,
      parentThreadId: Option[ThreadId],
      title: ThreadTitle,
      remarks: Option[ThreadRemarks],
      administratorIds: AdministratorIds,
      memberIds: MemberIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      CreateThread(
        ULID(),
        threadId,
        creatorId,
        parentThreadId,
        title,
        remarks,
        administratorIds,
        memberIds,
        createdAt,
        reply = false
      )
  }

  // --- スレッドの存在確認
  final case class ExistsThread(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      createAt: Instant
  ) extends CommandRequest
  sealed trait ExistsThreadResponse extends CommandResponse
  final case class ExistsThreadSucceeded(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      exists: Boolean,
      createAt: Instant
  ) extends ExistsThreadResponse
      with CommandSuccessResponse
  final case class ExistsThreadFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends ExistsThreadResponse
      with CommandFailureResponse

  // --- スレッドの破棄
  final case class DestroyThread(
      id: ULID,
      threadId: ThreadId,
      destroyerId: AccountId,
      createAt: Instant,
      reply: Boolean
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = destroyerId
    override def toEvent: Event      = ThreadDestroyed(ULID(), threadId, destroyerId, createAt)
  }
  sealed trait DestroyThreadResponse extends CommandResponse
  final case class DestroyThreadSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends DestroyThreadResponse
      with CommandSuccessResponse
  final case class DestroyThreadFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends DestroyThreadResponse
      with CommandFailureResponse
  final case class ThreadDestroyed(id: ULID, threadId: ThreadId, destroyerId: AccountId, createdAt: Instant)
      extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      DestroyThread(ULID(), threadId, destroyerId, createdAt, reply = false)
  }

  // --- 管理者の追加
  final case class JoinAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      administratorIds: AdministratorIds,
      createAt: Instant,
      reply: Boolean
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = adderId
    override def toEvent: Event      = AdministratorIdsJoined(ULID(), threadId, adderId, administratorIds, createAt)
  }
  sealed trait JoinAdministratorIdsResponse extends CommandResponse with CommandSuccessResponse
  final case class JoinAdministratorIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends JoinAdministratorIdsResponse
      with CommandFailureResponse
  final case class JoinAdministratorIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends JoinAdministratorIdsResponse
  final case class AdministratorIdsJoined(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      administratorIds: AdministratorIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      JoinAdministratorIds(ULID(), threadId, adderId, administratorIds, createdAt, reply = false)
  }

  // --- 管理者の削除
  final case class LeaveAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      administratorIds: AdministratorIds,
      createAt: Instant,
      reply: Boolean
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = removerId
    override def toEvent: Event      = AdministratorIdsLeft(ULID(), threadId, removerId, administratorIds, createAt)
  }
  sealed trait LeaveAdministratorIdsResponse extends CommandResponse
  final case class LeaveAdministratorIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends LeaveAdministratorIdsResponse
      with CommandSuccessResponse
  final case class LeaveAdministratorIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends LeaveAdministratorIdsResponse
      with CommandFailureResponse
  final case class AdministratorIdsLeft(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      administratorIds: AdministratorIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      LeaveAdministratorIds(ULID(), threadId, senderId, administratorIds, createdAt, reply = false)
  }

  // --- 管理者の取得
  final case class GetAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      createAt: Instant
  ) extends CommandRequest
  trait GetAdministratorIdsResponse extends CommandResponse
  final case class GetAdministratorIdsSucceeded(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      administratorIds: AdministratorIds,
      createAt: Instant
  ) extends GetAdministratorIdsResponse
      with CommandSuccessResponse
  final case class GetAdministratorIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends GetAdministratorIdsResponse
      with CommandFailureResponse

  // --- メンバーの追加
  final case class JoinMemberIds(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      memberIds: MemberIds,
      createAt: Instant,
      reply: Boolean
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = adderId
    override def toEvent: Event      = MemberIdsAdded(ULID(), threadId, adderId, memberIds, createAt)
  }
  sealed trait JoinMemberIdsResponse extends CommandResponse
  final case class JoinMemberIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends JoinMemberIdsResponse
      with CommandSuccessResponse
  final case class JoinMemberIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends JoinMemberIdsResponse
      with CommandFailureResponse
  final case class MemberIdsAdded(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      memberIds: MemberIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      JoinMemberIds(ULID(), threadId, adderId, memberIds, createdAt, reply = false)
  }

  // --- メンバーの削除
  final case class LeaveMemberIds(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      memberIds: MemberIds,
      createAt: Instant,
      reply: Boolean
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = removerId
    override def toEvent: Event      = MemberIdsLeft(ULID(), threadId, senderId, memberIds, createAt)
  }
  sealed trait LeaveMemberIdsResponse extends CommandResponse
  final case class LeaveMemberIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends LeaveMemberIdsResponse
      with CommandSuccessResponse
  final case class LeaveMemberIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends LeaveMemberIdsResponse
      with CommandFailureResponse
  final case class MemberIdsLeft(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      memberIds: MemberIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      LeaveMemberIds(ULID(), threadId, removerId, memberIds, createdAt, reply = false)
  }

  // --- メンバーの取得
  final case class GetMemberIds(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      createAt: Instant
  ) extends CommandRequest
  trait GetMemberIdsResponse extends CommandResponse
  final case class GetMemberIdsSucceeded(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      memberIds: MemberIds,
      createAt: Instant
  ) extends GetMemberIdsResponse
  final case class GetMemberIdsFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends GetMemberIdsResponse

  // --- メッセージの追加
  final case class AddMessages(
      id: ULID,
      threadId: ThreadId,
      messages: Messages,
      createAt: Instant,
      reply: Boolean
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = messages.breachEncapsulationOfValues.head.senderId
    override def toEvent: Event      = MessagesAdded(ULID(), threadId, messages, createAt)
  }
  sealed trait AddMessagesResponse extends CommandResponse
  final case class AddMessagesSucceeded(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      messageIds: MessageIds,
      createAt: Instant
  ) extends AddMessagesResponse
      with CommandSuccessResponse
  final case class AddMessagesFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends AddMessagesResponse
      with CommandFailureResponse
  final case class MessagesAdded(
      id: ULID,
      threadId: ThreadId,
      messages: Messages,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest = AddMessages(ULID(), threadId, messages, createdAt, reply = false)
  }

  // --- メッセージの削除
  final case class RemoveMessages(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      messageIds: MessageIds,
      createAt: Instant,
      reply: Boolean
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = removerId
    override def toEvent: Event      = MessagesRemoved(ULID(), threadId, removerId, messageIds, createAt)
  }
  sealed trait RemoveMessagesResponse extends CommandResponse
  final case class RemoveMessagesSucceeded(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      messageIds: MessageIds,
      createAt: Instant
  ) extends RemoveMessagesResponse
      with CommandSuccessResponse
  final case class RemoveMessagesFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends RemoveMessagesResponse
      with CommandFailureResponse
  final case class MessagesRemoved(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      messageIds: MessageIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      RemoveMessages(ULID(), threadId, removerId, messageIds, createdAt, reply = false)
  }

  // --- メッセージの取得
  final case class GetMessages(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      createAt: Instant
  ) extends CommandRequest
  trait GetMessagesResponse extends CommandResponse
  final case class GetMessagesSucceeded(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      messages: Messages,
      createAt: Instant
  ) extends GetMessagesResponse
  final case class GetMessagesFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends GetMessagesResponse

}
