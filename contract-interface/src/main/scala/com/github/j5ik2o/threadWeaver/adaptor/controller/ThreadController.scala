package com.github.j5ik2o.threadWeaver.adaptor.controller

import akka.http.scaladsl.server._
import com.github.j5ik2o.threadWeaver.adaptor.json.{ CreateThreadRequestJson, CreateThreadResponseJson }
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{ Content, Schema }
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import javax.ws.rs._
import kamon.context.Context

@Path("/v1")
@Produces(Array("application/json"))
trait ThreadController {

  def toRoutes(implicit context: Context): Route

  @POST
  @Path("/threads")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Create thread",
    description = "Create thread request",
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[CreateThreadRequestJson])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Create thread response",
        content = Array(new Content(schema = new Schema(implementation = classOf[CreateThreadResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def createThread(implicit context: Context): Route

}
