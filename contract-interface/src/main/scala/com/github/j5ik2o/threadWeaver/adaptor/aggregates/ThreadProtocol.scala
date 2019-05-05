package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import akka.actor.typed.ActorRef
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object ThreadProtocol {

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

  final case class AddSubscribers(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      subscribers: Seq[ActorRef[Message]],
      createAt: Instant
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = SubscribersAdded(ULID(), threadId, senderId, subscribers, createAt)
  }
  final case class SubscribersAdded(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      subscribers: Seq[ActorRef[Message]],
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest = AddSubscribers(ULID(), threadId, senderId, subscribers, createdAt)
  }

  // --- スレッドの生成
  final case class CreateThread(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      parentThreadId: Option[ThreadId],
      administratorIds: AdministratorIds,
      memberIds: MemberIds,
      createAt: Instant,
      replyTo: Option[ActorRef[CreateThreadResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event =
      ThreadCreated(ULID(), threadId, senderId, parentThreadId, administratorIds, memberIds, createAt)
  }
  sealed trait CreateThreadResponse extends CommandResponse
  final case class CreateThreadSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends CreateThreadResponse
  final case class CreateThreadFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends CreateThreadResponse
  final case class ThreadCreated(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      parentThreadId: Option[ThreadId],
      administratorIds: AdministratorIds,
      memberIds: MemberIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      CreateThread(ULID(), threadId, senderId, parentThreadId, administratorIds, memberIds, createdAt)
  }

  // --- スレッドの破棄
  final case class DestroyThread(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      createAt: Instant,
      replyTo: Option[ActorRef[DestroyThreadResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = ThreadDestroyed(ULID(), threadId, senderId, createAt)
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
  final case class ThreadDestroyed(id: ULID, threadId: ThreadId, senderId: AccountId, createdAt: Instant)
      extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest = DestroyThread(ULID(), threadId, senderId, createdAt)
  }

  // --- 管理者の追加
  final case class AddAdministratorIds(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      administratorIds: AdministratorIds,
      createAt: Instant,
      replyTo: Option[ActorRef[AddAdministratorIdsResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = AdministratorIdsAdded(ULID(), threadId, senderId, administratorIds, createAt)
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
      senderId: AccountId,
      administratorIds: AdministratorIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest =
      AddAdministratorIds(ULID(), threadId, senderId, administratorIds, createdAt)
  }

  // --- メンバーの追加
  final case class AddMemberIds(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      memberIds: MemberIds,
      createAt: Instant,
      replyTo: Option[ActorRef[AddMemberIdsResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = MemberIdsAdded(ULID(), threadId, senderId, memberIds, createAt)
  }
  sealed trait AddMemberIdsResponse extends CommandResponse
  final case class AddMemberIdsSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends AddMemberIdsResponse
  final case class AddMemberIdsFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends AddMemberIdsResponse
  final case class MemberIdsAdded(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      memberIds: MemberIds,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest = AddMemberIds(ULID(), threadId, senderId, memberIds, createdAt)
  }

  // --- メッセージの追加
  final case class AddMessages(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      messages: Messages,
      createAt: Instant,
      replyTo: Option[ActorRef[AddMessagesResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = MessagesAdded(ULID(), threadId, senderId, messages, createAt)
  }
  sealed trait AddMessagesResponse extends CommandResponse
  final case class AddMessagesSucceeded(id: ULID, requestId: ULID, threadId: ThreadId, createAt: Instant)
      extends AddMessagesResponse
  final case class AddMessagesFailed(id: ULID, requestId: ULID, threadId: ThreadId, message: String, createAt: Instant)
      extends AddMessagesResponse
  final case class MessagesAdded(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      messages: Messages,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest = AddMessages(ULID(), threadId, senderId, messages, createdAt)
  }

  // --- メッセージの削除
  final case class RemoveMessages(
      id: ULID,
      threadId: ThreadId,
      senderId: AccountId,
      messageIds: MessageIds,
      createAt: Instant,
      replyTo: Option[ActorRef[RemoveMessagesResponse]] = None
  ) extends CommandRequest
      with ToEvent {
    override def toEvent: Event = MessagesRemoved(ULID(), threadId, messageIds, senderId, createAt)
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
      messageIds: MessageIds,
      senderId: AccountId,
      createdAt: Instant
  ) extends Event
      with ToCommandRequest {
    override def toCommandRequest: CommandRequest = RemoveMessages(ULID(), threadId, senderId, messageIds, createdAt)
  }

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

}
