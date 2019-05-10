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
  @Path("/threads/new")
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
  @Path("/threads/{thread_id}/administrator-ids/add")
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
  @Path("/threads/{thread_id}/member-ids/add")
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

  @GET
  @Path("/threads/{thread_id}")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Get messages from thread",
    description = "Get messages request",
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
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Get thread response",
        content = Array(new Content(schema = new Schema(implementation = classOf[GetThreadResponseJson])))
      ),
      new ApiResponse(responseCode = "404", description = "Not found entity"),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def getThread(implicit context: Context): Route

  @GET
  @Path("/threads")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Get messages from thread",
    description = "Get messages request",
    parameters = Array(
      new Parameter(
        name = "offset",
        required = false,
        in = ParameterIn.QUERY,
        description = "offset",
        allowEmptyValue = false,
        schema = new Schema(
          `type` = "number",
          format = "int64"
        )
      ),
      new Parameter(
        name = "limit",
        required = false,
        in = ParameterIn.QUERY,
        description = "limit",
        allowEmptyValue = false,
        schema = new Schema(
          `type` = "number",
          format = "int64"
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Get threads response",
        content = Array(new Content(schema = new Schema(implementation = classOf[GetThreadsResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def getThreads(implicit context: Context): Route

  @GET
  @Path("/threads/{thread_id}/administrator-ids")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Get administrator-ids from thread",
    description = "Get administrator-ids request",
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
      ),
      new Parameter(
        name = "offset",
        required = false,
        in = ParameterIn.QUERY,
        description = "offset",
        allowEmptyValue = false,
        schema = new Schema(
          `type` = "number",
          format = "int64"
        )
      ),
      new Parameter(
        name = "limit",
        required = false,
        in = ParameterIn.QUERY,
        description = "limit",
        allowEmptyValue = false,
        schema = new Schema(
          `type` = "number",
          format = "int64"
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Get administrator-ids response",
        content =
          Array(new Content(schema = new Schema(implementation = classOf[GetThreadAdministratorIdsResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def getAdministratorIds(implicit context: Context): Route

  @GET
  @Path("/threads/{thread_id}/member-ids")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Get member-ids from thread",
    description = "Get member-ids request",
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
      ),
      new Parameter(
        name = "offset",
        required = false,
        in = ParameterIn.QUERY,
        description = "offset",
        allowEmptyValue = false,
        schema = new Schema(
          `type` = "number",
          format = "int64"
        )
      ),
      new Parameter(
        name = "limit",
        required = false,
        in = ParameterIn.QUERY,
        description = "limit",
        allowEmptyValue = false,
        schema = new Schema(
          `type` = "number",
          format = "int64"
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Get member-ids response",
        content = Array(new Content(schema = new Schema(implementation = classOf[GetThreadMemberIdsResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def getMemberIds(implicit context: Context): Route

  @GET
  @Path("/threads/{thread_id}/messages")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Get messages from thread",
    description = "Get messages request",
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
      ),
      new Parameter(
        name = "offset",
        required = false,
        in = ParameterIn.QUERY,
        description = "offset",
        allowEmptyValue = false,
        schema = new Schema(
          `type` = "number",
          format = "int64"
        )
      ),
      new Parameter(
        name = "limit",
        required = false,
        in = ParameterIn.QUERY,
        description = "limit",
        allowEmptyValue = false,
        schema = new Schema(
          `type` = "number",
          format = "int64"
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Get messages response",
        content = Array(new Content(schema = new Schema(implementation = classOf[GetThreadMessagesResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def getMessages(implicit context: Context): Route

}
