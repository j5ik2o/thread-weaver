package com.github.j5ik2o.threadWeaver.adaptor.error

sealed trait InterfaceError {
  val message: String
  val cause: Option[Throwable]
}

case class ULIDFormatError(message: String, cause: Option[Throwable] = None)        extends InterfaceError
case class ThreadIdFormatError(message: String, cause: Option[Throwable] = None)    extends InterfaceError
case class AdministratorIdsError(message: String, cause: Option[Throwable] = None)  extends InterfaceError
case class AccountIdFormatError(message: String, cause: Option[Throwable] = None)   extends InterfaceError
case class InstantFormatError(message: String, cause: Option[Throwable] = None)     extends InterfaceError
case class TextMessageFormatError(message: String, cause: Option[Throwable] = None) extends InterfaceError
