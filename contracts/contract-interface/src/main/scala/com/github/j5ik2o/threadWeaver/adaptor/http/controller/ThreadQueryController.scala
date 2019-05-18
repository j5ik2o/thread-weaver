package com.github.j5ik2o.threadWeaver.adaptor.http.controller

import akka.http.scaladsl.server.Route
import com.github.j5ik2o.threadWeaver.adaptor.http.json._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ Content, Schema }
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{ Operation, Parameter }
import javax.ws.rs.{ Consumes, GET, Path, Produces }

@Path("/v1")
@Produces(Array("application/json"))
trait ThreadQueryController {

  def toRoutes: Route

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
  private[controller] def getThread: Route

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
  private[controller] def getThreads: Route

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
  private[controller] def getAdministratorIds: Route

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
  private[controller] def getMemberIds: Route

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
  private[controller] def getMessages: Route

}
