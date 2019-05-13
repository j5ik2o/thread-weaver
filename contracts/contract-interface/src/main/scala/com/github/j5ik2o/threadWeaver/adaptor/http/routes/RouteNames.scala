package com.github.j5ik2o.threadWeaver.adaptor.http.routes

object RouteNames {
  final val Base: String                            = "threads"
  final val CreateThread: String                    = s"/$Base/create"
  final def DestroyThread(threadId: String): String = s"$Base/$threadId/destroy"
  final def JoinAdministratorIds(threadId: String, accountId: String): String =
    s"/$Base/$threadId/administrator-ids/join"
  final def LeaveAdministratorIds(threadId: String): String = s"/$Base/$threadId/administrator-ids/leave"

  final def JoinMemberIds(threadId: String): String  = s"/$Base/$threadId/member-ids/join"
  final def LeaveMemberIds(threadId: String): String = s"/$Base/$threadId/member-ids/leave"

  final def AddMessages(threadId: String): String    = s"/$Base/$threadId/messages/add"
  final def RemoveMessages(threadId: String): String = s"/$Base/$threadId/messages/remove"

  final def GetAdministratorIds(threadId: String, accountId: String): String =
    s"/$Base/$threadId/administrator-ids?account_id=$accountId"
  final def GetMemberIds(threadId: String, accountId: String): String =
    s"/$Base/$threadId/member-ids?account_id=$accountId"

  final def GetMessages(threadId: String, accountId: String): String =
    s"/$Base/$threadId/messages?account_id=$accountId"
  final def GetThreads(accountId: String): String                  = s"/$Base?account_id=$accountId"
  final def GetThread(threadId: String, accountId: String): String = s"/$Base/$threadId?account_id=$accountId"
}
