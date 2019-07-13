package com.github.j5ik2o.threadWeaver.useCase.untyped

import monix.eval.Task
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._

trait UseCaseSupport {

  val maxRetries = 3
  val firstDelay = 300 milliseconds

  private val logger = LoggerFactory.getLogger(getClass)

  def retryBackoff[A](
      sourceFuture: Future[A],
      maxRetries: Int,
      firstDelay: FiniteDuration,
      retryMessage: String = ""
  ): Task[A] = {
    Task.fromFuture(sourceFuture).onErrorHandleWith {
      case ex: akka.pattern.AskTimeoutException =>
        if (maxRetries > 0) {
          // Recursive call, it's OK as Monix is stack-safe
          logger.info(s"Retrying...: $retryMessage")
          retryBackoff(sourceFuture, maxRetries - 1, firstDelay * 2)
            .delayExecution(firstDelay)
        } else
          Task.raiseError(ex)
    }
  }

}
