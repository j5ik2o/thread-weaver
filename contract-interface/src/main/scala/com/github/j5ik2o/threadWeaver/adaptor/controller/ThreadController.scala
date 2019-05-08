package com.github.j5ik2o.threadWeaver.adaptor.controller

import akka.http.scaladsl.server._
import com.github.j5ik2o.threadWeaver.adaptor.json._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ Content, Schema }
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{ Operation, Parameter }
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

  @POST
  @Path("/threads/{thread_id}/administrator-ids")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Add administratorIds to thread",
    description = "Add administratorIds request",
    parameters = Array(
      new Parameter(
        name = "thread_id",
        required = true,
        in = ParameterIn.PATH,
        description = "threadId",
        allowEmptyValue = false,
        schema = new Schema(
          `type` = "string"
        )
      )
    ),
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[AddAdministratorIdsRequestJson])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Add administratorIds response",
        content = Array(new Content(schema = new Schema(implementation = classOf[AddAdministratorIdsResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def addAdministratorIds(implicit context: Context): Route

  @POST
  @Path("/threads/{thread_id}/member-ids")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Add memberIds to thread",
    description = "Add memberIds request",
    parameters = Array(
      new Parameter(
        name = "thread_id",
        required = true,
        in = ParameterIn.PATH,
        description = "threadId",
        allowEmptyValue = false,
        schema = new Schema(
          `type` = "string"
        )
      )
    ),
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[AddMemberIdsRequestJson])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Add memberIds response",
        content = Array(new Content(schema = new Schema(implementation = classOf[AddMemberIdsResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def addMemberIds(implicit context: Context): Route

}
