package com.github.j5ik2o.threadWeave.adaptor.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directive, Directive0, Directive1, RouteResult }
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._
import kamon.Kamon
import kamon.context.{ Context, Key }
import kamon.metric.{ CounterMetric, HistogramMetric, MeasurementUnit }

trait MetricsDirectives {

  val TraceName = Key.local[String]("traceName", "undefined")
  val Tags      = Key.local[Map[String, String]]("tags", Map.empty)

  def traceName(context: Context)(traceName: String, tags: Map[String, String] = Map.empty): Directive0 =
    mapRequest { request =>
      val newContext = context.withKey(TraceName, traceName).withKey(Tags, tags)
      Kamon.storeContext(newContext)
      request
    }

  val applicationName: String = "reaction"

  val requestCounter: CounterMetric =
    Kamon.counter(s"$applicationName.request.count")

  val processingTime: HistogramMetric =
    Kamon.histogram(s"$applicationName.request.processing-time", MeasurementUnit.time.nanoseconds)

  val errorResponseCounter: CounterMetric =
    Kamon.counter(s"$applicationName.request.errors")

  val rejectedResponseCounter: CounterMetric =
    Kamon.counter(s"$applicationName.request.rejections")

  def apiMetrics: Directive1[Context] = {
    extractRequestContext.flatMap { _ =>
      val context = Context.create()
      Directive[Unit] { inner => ctx =>
        val clock       = Kamon.clock()
        val timestamp   = clock.nanos()
        val context     = Kamon.currentContext()
        val traceName   = context.get(TraceName)
        val tags        = context.get(Tags) + (TraceName.name -> traceName)
        val spanBuilder = Kamon.buildSpan(traceName)
        val span = tags
          .foldLeft(spanBuilder) {
            case (sb, (k, v)) =>
              sb.withMetricTag(k, v)
          }.start
        inner(())(ctx).fast.transformWith(
          { result =>
            val context   = Kamon.currentContext()
            val traceName = context.get(TraceName)
            val tags      = context.get(Tags) + (TraceName.name -> traceName)
            processingTime.refine(tags).record(clock.nanos() - timestamp)
            span.finish
            requestCounter.refine(tags).increment()
            result match {
              case RouteResult.Complete(response) =>
                val status = response.status.intValue
                if (status >= StatusCodes.InternalServerError.intValue && status <= StatusCodes.NetworkConnectTimeout.intValue) {
                  errorResponseCounter.refine(tags).increment()
                } else if (status >= StatusCodes.BadRequest.intValue && status <= StatusCodes.UnavailableForLegalReasons.intValue) {
                  rejectedResponseCounter.refine(tags).increment()
                }
              case _: RouteResult.Rejected =>
                rejectedResponseCounter.refine(tags).increment()
            }
            FastFuture.successful(result)
          }, { e =>
            val context   = Kamon.currentContext()
            val traceName = context.get(TraceName)
            val tags      = context.get(Tags) + (TraceName.name -> traceName)
            processingTime.refine(tags).record(clock.nanos() - timestamp)
            span.addError("error.response", e)
            span.finish
            requestCounter.refine(tags).increment()
            errorResponseCounter.refine(tags).increment()
            FastFuture.failed(e)
          }
        )(ctx.executionContext)
      }.tflatMap { _ =>
        provide(context)
      }
    }
  }

}
