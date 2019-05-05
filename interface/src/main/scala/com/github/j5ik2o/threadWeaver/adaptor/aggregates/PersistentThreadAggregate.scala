package com.github.j5ik2o.threadWeaver.adaptor.aggregates

import java.time.Instant

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.persistence.typed.{ PersistenceId, RecoveryCompleted }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.domain.model.threads.{ Messages, Thread, ThreadId }
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

object PersistentThreadAggregate {

  def behavior(id: ThreadId, subscribers: Seq[ActorRef[Message]]): Behavior[CommandRequest] =
    Behaviors.setup[CommandRequest] { ctx =>
      subscribers.foreach(_ ! Started(ULID(), id, Instant.now, ctx.self))

      EventSourcedBehavior[CommandRequest, Event, State](
        persistenceId = PersistenceId(id.value.asString),
        emptyState = State(None, subscribers),
        commandHandler = {
          case (State(None, _), c @ CreateThread(requestId, threadId, _, _, _, _, createAt, replyTo)) =>
            Effect.persist(c.toEvent).thenRun { _ =>
              replyTo.foreach(_ ! CreateThreadSucceeded(ULID(), requestId, threadId, createAt))
            }
          case (State(_, _), c @ AddSubscribers(_, _, _, _, _)) =>
            Effect.persist(c.toEvent)
          case (State(Some(thread), _), c @ DestroyThread(requestId, threadId, senderId, createAt, replyTo)) =>
            thread.destroy(senderId, createAt) match {
              case Left(exception) =>
                Effect.none.thenRun { _ =>
                  replyTo.foreach(
                    _ ! DestroyThreadFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
                  )
                }
              case Right(_) =>
                Effect.persist(c.toEvent).thenRun { _ =>
                  replyTo.foreach(_ ! DestroyThreadSucceeded(ULID(), requestId, threadId, createAt))
                }
            }
          case (
              State(Some(thread), _),
              c @ AddAdministratorIds(requestId, threadId, senderId, administratorIds, createAt, replyTo)
              ) =>
            thread.addAdministratorIds(administratorIds, senderId) match {
              case Left(exception) =>
                Effect.none.thenRun { _ =>
                  replyTo.foreach(
                    _ ! AddAdministratorIdsFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
                  )
                }
              case Right(_) =>
                Effect.persist(c.toEvent).thenRun { _ =>
                  replyTo.foreach(_ ! AddAdministratorIdsSucceeded(ULID(), requestId, threadId, createAt))
                }
            }

          case (
              State(Some(thread), _),
              c @ AddMemberIds(requestId, threadId, senderId, memberIds, createAt, replyTo)
              ) =>
            thread.addMemberIds(memberIds, senderId) match {
              case Left(exception) =>
                Effect.none.thenRun { _ =>
                  replyTo.foreach(
                    _ ! AddMemberIdsFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
                  )
                }
              case Right(_) =>
                Effect.persist(c.toEvent).thenRun { _ =>
                  replyTo.foreach(_ ! AddMemberIdsSucceeded(ULID(), requestId, threadId, createAt))
                }
            }

          case (State(Some(thread), _), c @ AddMessages(requestId, threadId, senderId, messages, createAt, replyTo)) =>
            thread.addMessages(messages, senderId, createAt) match {
              case Left(exception) =>
                Effect.none.thenRun { _ =>
                  replyTo.foreach(_ ! AddMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt))
                }
              case Right(_) =>
                Effect.persist(c.toEvent).thenRun { _ =>
                  replyTo.foreach(_ ! AddMessagesSucceeded(ULID(), requestId, threadId, createAt))
                }
            }

          case (State(Some(thread), _), GetMessages(requestId, threadId, senderId, createAt, replyTo)) =>
            Effect.none.thenRun { _ =>
              thread.getMessages(senderId) match {
                case Left(exception) =>
                  replyTo ! GetMessagesFailed(ULID(), requestId, threadId, exception.getMessage, createAt)
                case Right(messages) =>
                  replyTo ! GetMessagesSucceeded(ULID(), requestId, threadId, messages, createAt)
              }
            }

          case _ =>
            Effect.none

        },
        eventHandler = {
          case (s @ State(None, _), e: ThreadCreated) =>
            s.copy(
              thread = Some(
                Thread(
                  e.threadId,
                  e.senderId,
                  e.parentThreadId,
                  e.administratorIds,
                  e.memberIds,
                  Messages.empty,
                  e.createdAt,
                  e.createdAt
                )
              )
            )
          case (s @ State(_, subscribers), e: SubscribersAdded) =>
            s.copy(subscribers = subscribers ++ e.subscribers)
          case (s @ State(Some(thread), _), e: ThreadDestroyed) =>
            s.copy(
              thread = Some(thread.destroy(e.senderId, e.createdAt).right.get)
            )
          case (s @ State(Some(thread), _), e: AdministratorIdsAdded) =>
            s.copy(
              thread = Some(thread.addAdministratorIds(e.administratorIds, e.senderId).right.get)
            )
          case (s @ State(Some(thread), _), e: MemberIdsAdded) =>
            s.copy(
              thread = Some(thread.addMemberIds(e.memberIds, e.senderId).right.get)
            )
          case (s @ State(Some(thread), _), e: MessagesAdded) =>
            s.copy(
              thread = Some(thread.addMessages(e.messages, e.senderId, e.createdAt).right.get)
            )
          case (state, _) =>
            state

        }
      ).receiveSignal {
        case (_, RecoveryCompleted) =>
          ctx.log.info("recovery completed")

      }
    }

  case class State(thread: Option[Thread], subscribers: Seq[ActorRef[Message]])

}
