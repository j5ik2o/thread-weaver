package com.github.j5ik2o.threadWeaver.adaptor.http.routes

object RouteNames {
  final val Base: String         = "threads"
  final val CreateThread: String = s"/$Base/create"

  final def JoinAdministratorIds(threadId: String): String  = s"/$Base/$threadId/administrator-ids/join"
  final def LeaveAdministratorIds(threadId: String): String = s"/$Base/$threadId/administrator-ids/leave"
  final def GetAdministratorIds(threadId: String): String   = s"/$Base/$threadId/administrator-ids"

  final def JoinMemberIds(threadId: String): String  = s"/$Base/$threadId/member-ids/join"
  final def LeaveMemberIds(threadId: String): String = s"/$Base/$threadId/member-ids/leave"
  final def GetMemberIds(threadId: String): String   = s"/$Base/$threadId/member-ids"

  final def AddMessages(threadId: String): String    = s"/$Base/$threadId/messages/add"
  final def RemoveMessages(threadId: String): String = s"/$Base/$threadId/messages/remove"
  final def GetMessages(threadId: String): String    = s"/$Base/$threadId/messages"
  final val GetThreads: String                       = Base
  final def GetThread(threadId: String): String      = s"/$Base/$threadId"
}
