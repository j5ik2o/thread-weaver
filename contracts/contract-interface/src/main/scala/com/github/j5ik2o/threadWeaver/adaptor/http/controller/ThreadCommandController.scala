package com.github.j5ik2o.threadWeaver.adaptor.http.controller

import akka.http.scaladsl.server._
import com.github.j5ik2o.threadWeaver.adaptor.http.json._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ Content, Schema }
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{ Operation, Parameter }
import javax.ws.rs._
import kamon.context.Context

@Path("/v1")
@Produces(Array("application/json"))
trait ThreadCommandController {

  def toRoutes(implicit context: Context): Route

  @POST
  @Path("threads/create")
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
  @Path("threads/{thread_id}/destroy")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Destroy thread",
    description = "Destroy thread request",
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
          schema = new Schema(implementation = classOf[DestroyThreadRequestJson])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Destroy thread response",
        content = Array(new Content(schema = new Schema(implementation = classOf[DestroyThreadResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def destroyThread(implicit context: Context): Route

  @POST
  @Path("/threads/{thread_id}/administrator-ids/join")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Join administratorIds to thread",
    description = "Join administratorIds request",
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
          schema = new Schema(implementation = classOf[JoinAdministratorIdsRequestJson])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Join administratorIds response",
        content = Array(new Content(schema = new Schema(implementation = classOf[JoinAdministratorIdsResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def joinAdministratorIds(implicit context: Context): Route

  @POST
  @Path("/threads/{thread_id}/administrator-ids/leave")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Leave administratorIds to thread",
    description = "Leave administratorIds request",
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
          schema = new Schema(implementation = classOf[LeaveAdministratorIdsRequestJson])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Leave administratorIds response",
        content = Array(new Content(schema = new Schema(implementation = classOf[LeaveAdministratorIdsResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def leaveAdministratorIds(implicit context: Context): Route

  @POST
  @Path("/threads/{thread_id}/member-ids/join")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Join memberIds to thread",
    description = "Join memberIds request",
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
          schema = new Schema(implementation = classOf[JoinMemberIdsRequestJson])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Join memberIds response",
        content = Array(new Content(schema = new Schema(implementation = classOf[JoinMemberIdsResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def joinMemberIds(implicit context: Context): Route

  @POST
  @Path("/threads/{thread_id}/member-ids/leave")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Leave memberIds to thread",
    description = "Leave memberIds request",
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
          schema = new Schema(implementation = classOf[LeaveMemberIdsRequestJson])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Leave memberIds response",
        content = Array(new Content(schema = new Schema(implementation = classOf[LeaveMemberIdsResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def leaveMemberIds(implicit context: Context): Route

  @POST
  @Path("/threads/{thread_id}/messages/add")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Add messages to thread",
    description = "Add messages request",
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
          schema = new Schema(implementation = classOf[AddMessagesRequestJson])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Add messages response",
        content = Array(new Content(schema = new Schema(implementation = classOf[AddMessagesResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def addMessages(implicit context: Context): Route

  @POST
  @Path("/threads/{thread_id}/messages/remove")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Remove messages to thread",
    description = "Remove messages request",
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
          schema = new Schema(implementation = classOf[RemoveMessagesRequestJson])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "remove messages response",
        content = Array(new Content(schema = new Schema(implementation = classOf[RemoveMessagesResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def removeMessages(implicit context: Context): Route

}
