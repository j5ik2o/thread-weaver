package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import akka.actor.typed.ActorRef
import cats.Id
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdaterProtocol
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object ThreadProtocol {

  type ThreadReadModelUpdaterRef = ActorRef[ThreadReadModelUpdaterProtocol.CommandRequest]
  type ThreadActorRefOfMessage   = ActorRef[ThreadProtocol.Message]
  type ThreadActorRefOfCommand   = ActorRef[ThreadProtocol.CommandRequest]

  sealed trait Message
  sealed trait Event extends Message {
    def id: ULID
    def threadId: ThreadId
    def createdAt: Instant
  }

  trait ToCommandRequest { this: Event =>
    def toCommandRequest: CommandRequest
  }
  sealed trait CommandMessage extends Message {
    def id: ULID
    def threadId: ThreadId
    def createAt: Instant
  }
  sealed trait CommandRequest extends CommandMessage {
    def senderId: AccountId
  }

  trait HasReplyTo[M[_], A] { this: CommandRequest =>
    def replyTo: M[ActorRef[A]]

    def withReplyTo(value: M[ActorRef[A]]): CommandRequest

  }

  trait ToEvent { this: CommandRequest =>
    def toEvent: Event
  }
  sealed trait CommandResponse extends CommandMessage {
    def requestId: ULID
  }
  trait CommandSuccessResponse extends CommandResponse
  trait CommandFailureResponse extends CommandResponse

//  final case class AddSubscribers(
//      id: ULID,
//      threadId: ThreadId,
//      senderId: AccountId,
//      subscribers: Seq[ActorRef[Message]],
//      createAt: Instant
//  ) extends CommandRequest
//      with ToEvent {
//    override def toEvent: Event = SubscribersAdded(ULID(), threadId, senderId, subscribers, createAt)
//  }
//  final case class SubscribersAdded(
//      id: ULID,
//      threadId: ThreadId,
//      senderId: AccountId,
//      subscribers: Seq[ActorRef[Message]],
//      createdAt: Instant
//  ) extends Event
//      with ToCommandRequest {
//    override def toCommandRequest: CommandRequest = AddSubscribers(ULID(), threadId, senderId, subscribers, createdAt)
//  }

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
      replyTo: Option[ActorRef[CreateThreadResponse]] = None
  ) extends CommandRequest
      with ToEvent
      with HasReplyTo[Option, CreateThreadResponse] {
    override def senderId: AccountId = creatorId
    override def toEvent: Event =
      ThreadCreated(ULID(), threadId, creatorId, parentThreadId, title, remarks, administratorIds, memberIds, createAt)
    override def withReplyTo(value: Option[ActorRef[CreateThreadResponse]]): CreateThread =
      copy(replyTo = value)
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
      CreateThread(ULID(), threadId, creatorId, parentThreadId, title, remarks, administratorIds, memberIds, createdAt)
  }

  final case class ExistsThread(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      createAt: Instant,
      replyTo: ActorRef[ExistsThreadResponse]
  ) extends CommandRequest
      with HasReplyTo[Id, ExistsThreadResponse] {
    override def withReplyTo(value: ActorRef[ExistsThreadResponse]): ExistsThread =
      copy(replyTo = value)
  }
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
      replyTo: Option[ActorRef[DestroyThreadResponse]] = None
  ) extends CommandRequest
      with ToEvent
      with HasReplyTo[Option, DestroyThreadResponse] {
    override def senderId: AccountId = destroyerId
    override def toEvent: Event      = ThreadDestroyed(ULID(), threadId, destroyerId, createAt)
    override def withReplyTo(value: Option[ActorRef[DestroyThreadResponse]]): DestroyThread =
      copy(replyTo = value)
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
    override def toCommandRequest: CommandRequest = DestroyThread(ULID(), threadId, destroyerId, createdAt)
  }

  // --- 管理者の追加
  final case class JoinAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      administratorIds: AdministratorIds,
      createAt: Instant,
      replyTo: Option[ActorRef[JoinAdministratorIdsResponse]] = None
  ) extends CommandRequest
      with ToEvent
      with HasReplyTo[Option, JoinAdministratorIdsResponse] {
    override def senderId: AccountId = adderId
    override def toEvent: Event      = AdministratorIdsJoined(ULID(), threadId, adderId, administratorIds, createAt)
    override def withReplyTo(value: Option[ActorRef[JoinAdministratorIdsResponse]]): JoinAdministratorIds =
      copy(replyTo = value)
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
      JoinAdministratorIds(ULID(), threadId, adderId, administratorIds, createdAt)
  }

  // --- 管理者の削除
  final case class LeaveAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      administratorIds: AdministratorIds,
      createAt: Instant,
      replyTo: Option[ActorRef[LeaveAdministratorIdsResponse]] = None
  ) extends CommandRequest
      with ToEvent
      with HasReplyTo[Option, LeaveAdministratorIdsResponse] {
    override def senderId: AccountId = removerId
    override def toEvent: Event      = AdministratorIdsLeft(ULID(), threadId, removerId, administratorIds, createAt)
    override def withReplyTo(value: Option[ActorRef[LeaveAdministratorIdsResponse]]): LeaveAdministratorIds =
      copy(replyTo = value)
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
      LeaveAdministratorIds(ULID(), threadId, senderId, administratorIds, createdAt)
  }

  // --- 管理者の取得
  final case class GetAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      createAt: Instant,
      replyTo: ActorRef[GetAdministratorIdsResponse]
  ) extends CommandRequest
      with HasReplyTo[Id, GetAdministratorIdsResponse] {
    override def withReplyTo(value: Id[ActorRef[GetAdministratorIdsResponse]]): GetAdministratorIds =
      copy(replyTo = value)
  }
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
      replyTo: Option[ActorRef[JoinMemberIdsResponse]] = None
  ) extends CommandRequest
      with ToEvent
      with HasReplyTo[Option, JoinMemberIdsResponse] {
    override def senderId: AccountId = adderId
    override def toEvent: Event      = MemberIdsAdded(ULID(), threadId, adderId, memberIds, createAt)
    override def withReplyTo(value: Option[ActorRef[JoinMemberIdsResponse]]): JoinMemberIds =
      copy(replyTo = value)
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
    override def toCommandRequest: CommandRequest = JoinMemberIds(ULID(), threadId, adderId, memberIds, createdAt)
  }

  // --- メンバーの削除
  final case class LeaveMemberIds(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      memberIds: MemberIds,
      createAt: Instant,
      replyTo: Option[ActorRef[LeaveMemberIdsResponse]] = None
  ) extends CommandRequest
      with ToEvent
      with HasReplyTo[Option, LeaveMemberIdsResponse] {
    override def senderId: AccountId = removerId
    override def toEvent: Event      = MemberIdsLeft(ULID(), threadId, senderId, memberIds, createAt)
    override def withReplyTo(value: Option[ActorRef[LeaveMemberIdsResponse]]): LeaveMemberIds =
      copy(replyTo = value)
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
    override def toCommandRequest: CommandRequest = LeaveMemberIds(ULID(), threadId, removerId, memberIds, createdAt)
  }

  // --- メンバーの取得
  final case class GetMemberIds(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      createAt: Instant,
      replyTo: ActorRef[GetMemberIdsResponse]
  ) extends CommandRequest
      with HasReplyTo[Id, GetMemberIdsResponse] {
    override def withReplyTo(value: ActorRef[GetMemberIdsResponse]): GetMemberIds =
      copy(replyTo = value)
  }
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
      replyTo: Option[ActorRef[AddMessagesResponse]] = None
  ) extends CommandRequest
      with ToEvent
      with HasReplyTo[Option, AddMessagesResponse] {
    override def senderId: AccountId = messages.breachEncapsulationOfValues.head.senderId
    override def toEvent: Event      = MessagesAdded(ULID(), threadId, messages, createAt)
    override def withReplyTo(value: Option[ActorRef[AddMessagesResponse]]): AddMessages =
      copy(replyTo = value)
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
    override def toCommandRequest: CommandRequest = AddMessages(ULID(), threadId, messages, createdAt)
  }

  // --- メッセージの削除
  final case class RemoveMessages(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      messageIds: MessageIds,
      createAt: Instant,
      replyTo: Option[ActorRef[RemoveMessagesResponse]] = None
  ) extends CommandRequest
      with ToEvent
      with HasReplyTo[Option, RemoveMessagesResponse] {
    override def senderId: AccountId = removerId
    override def toEvent: Event      = MessagesRemoved(ULID(), threadId, removerId, messageIds, createAt)
    override def withReplyTo(value: Option[ActorRef[RemoveMessagesResponse]]): CommandRequest =
      copy(replyTo = value)
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
    override def toCommandRequest: CommandRequest = RemoveMessages(ULID(), threadId, removerId, messageIds, createdAt)
  }

  // --- メッセージの取得
  final case class GetMessages(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      createAt: Instant,
      replyTo: ActorRef[GetMessagesResponse]
  ) extends CommandRequest
      with HasReplyTo[Id, GetMessagesResponse] {
    override def withReplyTo(value: Id[ActorRef[GetMessagesResponse]]): CommandRequest =
      copy(replyTo = value)
  }
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

  case object Idle extends CommandRequest {
    override def id: ULID            = throw new UnsupportedOperationException
    override def threadId: ThreadId  = throw new UnsupportedOperationException
    override def senderId: AccountId = throw new UnsupportedOperationException
    override def createAt: Instant   = throw new UnsupportedOperationException

  }

  case object Stop extends CommandRequest {
    override def id: ULID            = throw new UnsupportedOperationException
    override def threadId: ThreadId  = throw new UnsupportedOperationException
    override def senderId: AccountId = throw new UnsupportedOperationException
    override def createAt: Instant   = throw new UnsupportedOperationException
  }

  case class Started(id: ULID, threadId: ThreadId, createdAt: Instant, sender: ActorRef[Nothing]) extends Event

  case class Stopped(id: ULID, threadId: ThreadId, createdAt: Instant, sender: ActorRef[Nothing]) extends Event

}
