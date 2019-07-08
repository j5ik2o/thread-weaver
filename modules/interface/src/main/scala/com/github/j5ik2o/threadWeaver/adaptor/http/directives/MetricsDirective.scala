package com.github.j5ik2o.threadWeaver.adaptor.http.directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._
import kamon.Kamon
import kamon.metric.instrument.{ Counter, Histogram, Time }
import kamon.trace.TraceContext
import kamon.util.RelativeNanoTimestamp

object MetricsDirective {

  implicit class MetricsContext(val traceContext: TraceContext) extends AnyVal {

    def tagsWithTraceName: Map[String, String] = {
      traceContext.tags + ("traceName" -> traceContext.name)
    }
  }

}

trait MetricsDirective {

  import MetricsDirective._

  def traceName(context: TraceContext, name: String, tags: Map[String, String] = Map.empty): Directive0 = mapRequest {
    req =>
      tags.foreach { case (key, value) ⇒ context.addTag(key, value) }
      context.rename(name)
      req
  }

  def getRequestCounter(tags: Map[String, String]): Counter =
    Kamon.metrics.counter("request.count", tags)

  def getProcessingTime(tags: Map[String, String]): Histogram =
    Kamon.metrics.histogram("request.processing-time", tags, Time.Nanoseconds)

  def getErrorResponseCounter(tags: Map[String, String]): Counter =
    Kamon.metrics.counter("request.errors", tags)

  def getRejectedResponseCounter(tags: Map[String, String]): Counter =
    Kamon.metrics.counter("request.rejections", tags)

  def apiMetrics: Directive1[TraceContext] = {
    extractRequestContext.flatMap { ctx =>
      val traceContext = Kamon.tracer.newContext("api-trace") // 各エンドポイントでrenameされる
      val timestamp    = RelativeNanoTimestamp.now
      Directive[Unit] { inner => ctx =>
        inner(())(ctx).fast.transformWith(
          { result =>
            val tags = traceContext.tagsWithTraceName
            getRequestCounter(tags).increment()
            getProcessingTime(tags).record((RelativeNanoTimestamp.now - timestamp).nanos)
            result match {
              case RouteResult.Complete(response) =>
                val status = response.status.intValue
                if (status >= StatusCodes.InternalServerError.intValue && status <= StatusCodes.NetworkConnectTimeout.intValue) {
                  getErrorResponseCounter(tags).increment()
                } else if (status >= StatusCodes.BadRequest.intValue && status <= StatusCodes.UnavailableForLegalReasons.intValue) {
                  getRejectedResponseCounter(tags).increment()
                }
              case _: RouteResult.Rejected =>
                getRejectedResponseCounter(tags).increment()
            }
            traceContext.finish()
            FastFuture.successful(result)
          }, { e =>
            val tags = traceContext.tagsWithTraceName
            getRequestCounter(tags).increment()
            getProcessingTime(tags).record((RelativeNanoTimestamp.now - timestamp).nanos)
            getErrorResponseCounter(tags).increment()
            traceContext.finishWithError(e)
            FastFuture.failed(e)
          }
        )(ctx.executionContext)
      }.tflatMap { _ =>
        provide(traceContext)
      }
    }
  }

  def withSegment1[T](
      context: TraceContext,
      directive: Directive1[T]
  )(segmentName: String, category: String, library: String, tags: Map[String, String] = Map.empty): Directive1[T] = {
    val segment = context.startSegment(segmentName, category, library, tags)
    directive
      .flatMap { v =>
        segment.finish()
        provide(v)
      }
      .recover { rejections =>
        segment.finish()
        reject(rejections: _*)
      }
  }

  def withSegment0(
      context: TraceContext,
      directive: Directive0
  )(segmentName: String, category: String, library: String, tags: Map[String, String] = Map.empty): Directive0 = {
    val segment = context.startSegment(segmentName, category, library, tags)
    directive
      .tflatMap { _ =>
        segment.finish()
        pass
      }
      .recover { rejections =>
        segment.finish()
        reject(rejections: _*)
      }
  }

  def withErrorMetrics(counter: Counter): Directive0 = {
    import akka.http.scaladsl.util.FastFuture._
    Directive { innerRouteBuilder => ctx =>
      import ctx.executionContext
      innerRouteBuilder(())(ctx).fast.recoverWith {
        case e: Throwable =>
          counter.increment()
          FastFuture.failed(e)
      }
    }
  }

}
