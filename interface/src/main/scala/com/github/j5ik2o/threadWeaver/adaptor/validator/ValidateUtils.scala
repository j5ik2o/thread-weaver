package com.github.j5ik2o.threadWeaver.adaptor.validator

import java.time.Instant

import cats.implicits._
import com.github.j5ik2o.threadWeaver.adaptor.error.{
  AdministratorIdsError,
  InstantFormatError,
  TextMessageFormatError,
  ULIDFormatError
}
import com.github.j5ik2o.threadWeaver.domain.model.accounts.AccountId
import com.github.j5ik2o.threadWeaver.domain.model.threads._
import com.github.j5ik2o.threadWeaver.infrastructure.ulid.ULID

import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

object ValidateUtils {

  def validateULID(value: String): ValidationResult[ULID] = {
    ULID.parseFromString(value) match {
      case Success(result) => result.validNel
      case Failure(ex)     => ULIDFormatError(ex.getMessage).invalidNel
    }
  }

  def validateThreadIdOpt(value: Option[String]): ValidationResult[Option[ThreadId]] =
    value match {
      case Some(v) => validateThreadId(v).map(v => Some(v))
      case None    => None.validNel
    }

  def validateThreadId(value: String): ValidationResult[ThreadId] =
    validateULID(value).map(ThreadId)

  def validateAccountId(value: String): ValidationResult[AccountId] =
    validateULID(value).map(AccountId)

  def validateAdministratorIds(values: Seq[String]): ValidationResult[AdministratorIds] = {
    if (values.isEmpty)
      AdministratorIdsError("values is empty").invalidNel
    else {
      val result = values.map(validateAccountId).toList.sequence
      result.map(v => AdministratorIds(v.head, v.tail))
    }
  }

  def validateMemberIds(values: Seq[String]): ValidationResult[MemberIds] = {
    values.map(validateAccountId).toList.sequence.map(v => MemberIds(v: _*))
  }

  def validateText(value: String): ValidationResult[Text] = {
    Text.parseFrom(value).leftMap(e => TextMessageFormatError(e.getMessage)) match {
      case Left(e)  => e.invalidNel
      case Right(r) => r.validNel
    }
  }

  def validateMessageId(value: String): ValidationResult[MessageId] = {
    validateULID(value).map(MessageId)
  }

  def validateMessageIds(values: Seq[String]): ValidationResult[MessageIds] = {
    values.map(validateMessageId).toList.sequence.map(v => MessageIds(v: _*))
  }

  def validateMessageIdOpt(value: Option[String]): ValidationResult[Option[MessageId]] = {
    value match {
      case Some(v) => validateMessageId(v).map(v => Some(v))
      case None    => None.validNel
    }
  }

  def validateToAccountIds(values: Seq[String]): ValidationResult[ToAccountIds] = {
    values.map(validateAccountId).toList.sequence.map(v => ToAccountIds(v: _*))
  }

  def validateTextMessage(
      replyMessageIdValue: Option[String],
      toAccountIdsValues: Seq[String],
      textValue: String,
      senderId: String
  ): ValidationResult[TextMessage] = {
    (
      validateMessageIdOpt(replyMessageIdValue),
      validateToAccountIds(toAccountIdsValues),
      validateText(textValue),
      validateAccountId(senderId)
    ).mapN {
        case (replyMessageIdOpt, toAccountIds, text, senderId) =>
          val now = Instant.now
          TextMessage(MessageId(), replyMessageIdOpt, toAccountIds, text, senderId, now, now)
      }
  }

  def validateInstant(value: Long): ValidationResult[Instant] = {
    try {
      Instant.ofEpochMilli(value).validNel
    } catch {
      case NonFatal(ex) =>
        InstantFormatError(ex.getMessage).invalidNel
    }
  }

}
