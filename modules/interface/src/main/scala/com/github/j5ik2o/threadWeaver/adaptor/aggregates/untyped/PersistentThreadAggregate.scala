package com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped

import java.time.Instant

import akka.actor.SupervisorStrategy.Stop
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ typed, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated }
import akka.persistence.{ PersistentActor, RecoveryCompleted }
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.ThreadCommonProtocol
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.PersistentThreadAggregate.ReadModelUpdaterConfig
import com.github.j5ik2o.threadWeaver.adaptor.aggregates.untyped.ThreadProtocol._
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.ThreadReadModelUpdater.ReadJournalType
import com.github.j5ik2o.threadWeaver.adaptor.readModelUpdater.{
  ThreadReadModelUpdater,
  ThreadReadModelUpdaterProtocol
}
import com.github.j5ik2o.threadWeaver.domain.model.threads.ThreadId
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID
import kamon.Kamon
import kamon.trace.TraceContext
import slick.jdbc.JdbcProfile

object PersistentThreadAggregate {

  case class ReadModelUpdaterConfig(
      readJournal: ReadJournalType,
      profile: JdbcProfile,
      db: JdbcProfile#Backend#Database,
      sqlBatchSize: Long
  )

  def props: Option[ReadModelUpdaterConfig] => ThreadId => Seq[ActorRef] => Props =
    readModelUpdaterConfig =>
      id =>
        subscribers =>
          Props(new PersistentThreadAggregate(id, subscribers, ThreadAggregate.props, readModelUpdaterConfig))

}

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
class PersistentThreadAggregate(
    id: ThreadId,
    subscribers: Seq[ActorRef],
    propsF: (ThreadId, Seq[ActorRef]) => Props,
    readModelUpdaterConfig: Option[ReadModelUpdaterConfig]
) extends PersistentActor
    with ActorLogging {

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: Throwable =>
      Stop
  }

  private val ReadModelUpdaterRefName = "RoomReadModelUpdater"

  private def generateReadModelUpdaterRef: Option[typed.ActorRef[ThreadReadModelUpdaterProtocol.CommandRequest]] =
    readModelUpdaterConfig.map { config =>
      val readModelUpdaterBehavior =
        new ThreadReadModelUpdater(config.readJournal, config.profile, config.db).behavior(config.sqlBatchSize)
      val result = context.spawn(readModelUpdaterBehavior, ReadModelUpdaterRefName)
      context.watch(result)
      result
    }

  private var readModelUpdaterRef = generateReadModelUpdaterRef

  private var recoveryContext: TraceContext = _

  private val childRef =
    context.actorOf(propsF(id, subscribers), name = ThreadAggregate.name(id))

  context.watch(childRef)

  override def preStart(): Unit = {
    super.preStart()
    readModelUpdaterRef.foreach(_ ! ThreadReadModelUpdaterProtocol.Start(ULID(), id, Instant.now()))
    recoveryContext = Kamon.tracer.newContext("recovery")
  }

  override def postStop(): Unit = {
    readModelUpdaterRef.foreach(_ ! ThreadReadModelUpdaterProtocol.Stop(ULID(), id, Instant.now()))
    super.postStop()
  }
  override def persistenceId: String = ThreadAggregate.name(id)

  override def receiveRecover: Receive = {
    case e: ThreadCommonProtocol.Event with ToCommandRequest =>
      childRef ! e.toCommandRequest
    case RecoveryCompleted =>
      recoveryContext.finish()
      log.debug("recovery completed")
  }

  private def sending(commandContext: TraceContext, replyTo: ActorRef, event: ThreadCommonProtocol.Event): Receive = {
    case s: CommandSuccessResponse =>
      val segment = commandContext.startSegment("persist", "interface-adaptor", "thread-weaver")
      persistAsync(event) { _ =>
        log.info(s"persist: $event")
        replyTo ! s
        unstashAll()
        context.unbecome()
        segment.finish()
        commandContext.finish()
      }
    case f: CommandFailureResponse =>
      replyTo ! f
      unstashAll()
      context.unbecome()
      commandContext.finishWithError(new Exception(f.message))
    case _ =>
      stash()
  }

  override def receiveCommand: Receive = {
    case Terminated(c) if c == childRef =>
      context.stop(self)
    case Terminated(c) if readModelUpdaterRef.contains(c) =>
      readModelUpdaterRef = generateReadModelUpdaterRef
    case m: CommandRequest with ToEvent =>
      val commandContext = Kamon.tracer.newContext("command")
      childRef ! m
      context.become(sending(commandContext, sender(), m.toEvent))
    case m =>
      childRef forward m
  }

}
