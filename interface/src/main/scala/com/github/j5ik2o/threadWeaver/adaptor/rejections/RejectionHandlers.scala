package com.github.j5ik2o.threadWeaver.adaptor.rejections

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.RejectionHandler
import com.github.j5ik2o.threadWeaver.adaptor.directives.ValidationsRejection
import com.github.j5ik2o.threadWeaver.adaptor.json.ValidationErrorsResponseJson
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

object RejectionHandlers {

  final val default: RejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case ValidationsRejection(errors) =>
        complete((StatusCodes.BadRequest, ValidationErrorsResponseJson(errors.map(_.message).toList)))
    }
    .result()

}
