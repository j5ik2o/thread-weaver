package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import akka.actor.typed.ActorRef
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

  trait ToEvent { this: CommandRequest =>
    def toEvent: Event
  }
  sealed trait CommandResponse extends CommandMessage {
    def requestId: ULID
  }

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
      administratorIds: AdministratorIds,
      memberIds: MemberIds,
      createAt: Instant,
      replyTo: Option[ActorRef[CreateThreadResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = creatorId
    override def toEvent: Event =
      ThreadCreated(ULID(), threadId, creatorId, parentThreadId, administratorIds, memberIds, createAt)
  }
  sealed trait CreateThreadResponse extends CommandResponse
  final case class CreateThreadSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends CreateThreadResponse
  final case class CreateThreadFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends CreateThreadResponse
  final case class ThreadCreated(
      id: ULID,
      threadId: ThreadId,
      creatorId: AccountId,
      parentThreadId: Option[ThreadId],
      administratorIds: AdministratorIds,
      memberIds: MemberIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      CreateThread(ULID(), threadId, creatorId, parentThreadId, administratorIds, memberIds, createdAt)
  }

  // --- スレッドの破棄
  final case class DestroyThread(
      id: ULID,
      threadId: ThreadId,
      destroyerId: AccountId,
      createAt: Instant,
      replyTo: Option[ActorRef[DestroyThreadResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = destroyerId
    override def toEvent: Event      = ThreadDestroyed(ULID(), threadId, destroyerId, createAt)
  }
  sealed trait DestroyThreadResponse extends CommandResponse
  final case class DestroyThreadSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends DestroyThreadResponse
  final case class DestroyThreadFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends DestroyThreadResponse
  final case class ThreadDestroyed(id: ULID, threadId: ThreadId, destroyerId: AccountId, createdAt: Instant)
      extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest = DestroyThread(ULID(), threadId, destroyerId, createdAt)
  }

  // --- 管理者の追加
  final case class AddAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      administratorIds: AdministratorIds,
      createAt: Instant,
      replyTo: Option[ActorRef[AddAdministratorIdsResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = adderId
    override def toEvent: Event      = AdministratorIdsAdded(ULID(), threadId, adderId, administratorIds, createAt)
  }
  sealed trait AddAdministratorIdsResponse extends CommandResponse
  final case class AddAdministratorIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends AddAdministratorIdsResponse
  final case class AddAdministratorIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends AddAdministratorIdsResponse
  final case class AdministratorIdsAdded(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      administratorIds: AdministratorIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      AddAdministratorIds(ULID(), threadId, adderId, administratorIds, createdAt)
  }

  // --- 管理者の削除
  final case class RemoveAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      administratorIds: AdministratorIds,
      createAt: Instant,
      replyTo: Option[ActorRef[RemoveAdministratorIdsResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = removerId
    override def toEvent: Event      = AdministratorIdsRemoved(ULID(), threadId, removerId, administratorIds, createAt)

  }
  sealed trait RemoveAdministratorIdsResponse extends CommandResponse
  final case class RemoveAdministratorIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends RemoveAdministratorIdsResponse
  final case class RemoveAdministratorIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends RemoveAdministratorIdsResponse
  final case class AdministratorIdsRemoved(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      administratorIds: AdministratorIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      RemoveAdministratorIds(ULID(), threadId, senderId, administratorIds, createdAt)
  }

  // --- 管理者の取得
  final case class GetAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      createAt: Instant,
      replyTo: ActorRef[GetAdministratorIdsResponse]
  ) extends CommandRequest
  trait GetAdministratorIdsResponse extends CommandResponse
  final case class GetAdministratorIdsSucceeded(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      administratorIds: AdministratorIds,
      createAt: Instant
  ) extends GetAdministratorIdsResponse
  final case class GetAdministratorIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends GetAdministratorIdsResponse

  // --- メンバーの追加
  final case class AddMemberIds(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      memberIds: MemberIds,
      createAt: Instant,
      replyTo: Option[ActorRef[AddMemberIdsResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = adderId
    override def toEvent: Event      = MemberIdsAdded(ULID(), threadId, adderId, memberIds, createAt)
  }
  sealed trait AddMemberIdsResponse extends CommandResponse
  final case class AddMemberIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends AddMemberIdsResponse
  final case class AddMemberIdsFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends AddMemberIdsResponse
  final case class MemberIdsAdded(
      id: ULID,
      threadId: ThreadId,
      adderId: AccountId,
      memberIds: MemberIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest = AddMemberIds(ULID(), threadId, adderId, memberIds, createdAt)
  }

  // --- メンバーの削除
  final case class RemoveMemberIds(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      memberIds: MemberIds,
      createAt: Instant,
      replyTo: Option[ActorRef[RemoveMemberIdsResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def senderId: AccountId = removerId
    override def toEvent: Event      = MemberIdsRemoved(ULID(), threadId, senderId, memberIds, createAt)
  }
  sealed trait RemoveMemberIdsResponse extends CommandResponse
  final case class RemoveMemberIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends RemoveMemberIdsResponse
  final case class RemoveMemberIdsFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends RemoveMemberIdsResponse
  final case class MemberIdsRemoved(
      id: ULID,
      threadId: ThreadId,
      removerId: AccountId,
      memberIds: MemberIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest = RemoveMemberIds(ULID(), threadId, removerId, memberIds, createdAt)
  }

  // --- メンバーの取得
  final case class GetMemberIds(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      createAt: Instant,
      replyTo: ActorRef[GetMemberIdsResponse]
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
      replyTo: Option[ActorRef[AddMessagesResponse]] = None
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
  final case class AddMessagesFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends AddMessagesResponse
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
      with ToEvent {
    override def senderId: AccountId = removerId
    override def toEvent: Event      = MessagesRemoved(ULID(), threadId, removerId, messageIds, createAt)
  }
  sealed trait RemoveMessagesResponse extends CommandResponse
  final case class RemoveMessagesSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends RemoveMessagesResponse
  final case class RemoveMessagesFailed(
      id: ULID,
      requestId: ULID,
      threadId: ThreadId,
      message: String,
      createAt: Instant
  ) extends RemoveMessagesResponse
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
