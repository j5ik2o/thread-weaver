package com.github.j5ik2o.threadWeaver.adaptor.grpc.service

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.typed.scaladsl.ActorMaterializer
import cats.data.NonEmptyList
import com.github.j5ik2o.threadWeaver.adaptor.error.InterfaceError
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model._
import com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter._
import com.github.j5ik2o.threadWeaver.adaptor.grpc.validator.ThreadValidatorSupport
import com.github.j5ik2o.threadWeaver.adaptor.grpc.validator.ThreadValidatorSupport._
import com.github.j5ik2o.threadWeaver.useCase._
import wvlet.airframe.bind

import scala.concurrent.Future

trait ThreadCommandServiceImpl extends ThreadCommandService with ThreadValidatorSupport {

  private implicit val system: ActorSystem[Nothing]        = bind[ActorSystem[Nothing]]
  private val createThreadUseCase: CreateThreadUseCase     = bind[CreateThreadUseCase]
  private val createThreadPresenter: CreateThreadPresenter = bind[CreateThreadPresenter]

  private val destroyThreadUseCase: DestroyThreadUseCase     = bind[DestroyThreadUseCase]
  private val destroyThreadPresenter: DestroyThreadPresenter = bind[DestroyThreadPresenter]

  private val joinAdministratorIdsUseCase: JoinAdministratorIdsUseCase     = bind[JoinAdministratorIdsUseCase]
  private val joinAdministratorIdsPresenter: JoinAdministratorIdsPresenter = bind[JoinAdministratorIdsPresenter]

  private val leaveAdministratorIdsUseCase: LeaveAdministratorIdsUseCase     = bind[LeaveAdministratorIdsUseCase]
  private val leaveAdministratorIdsPresenter: LeaveAdministratorIdsPresenter = bind[LeaveAdministratorIdsPresenter]

  private val joinMemberIdsUseCase: JoinMemberIdsUseCase     = bind[JoinMemberIdsUseCase]
  private val joinMemberIdsPresenter: JoinMemberIdsPresenter = bind[JoinMemberIdsPresenter]

  private val leaveMemberIdsUseCase: LeaveMemberIdsUseCase     = bind[LeaveMemberIdsUseCase]
  private val leaveMemberIdsPresenter: LeaveMemberIdsPresenter = bind[LeaveMemberIdsPresenter]

  private val addMessagesUseCase: AddMessagesUseCase     = bind[AddMessagesUseCase]
  private val addMessagesPresenter: AddMessagesPresenter = bind[AddMessagesPresenter]

  private val removeMessagesUseCase: RemoveMessagesUseCase     = bind[RemoveMessagesUseCase]
  private val removeMessagesPresenter: RemoveMessagesPresenter = bind[RemoveMessagesPresenter]

  implicit val mat: ActorMaterializer = ActorMaterializer()

  private def errorResponse[A](apply: Seq[String] => A): NonEmptyList[InterfaceError] => Future[A] = {
    f: NonEmptyList[InterfaceError] =>
      Future.successful(apply(f.map(_.message).toList))
  }

  override def createThread(in: CreateThreadRequest): Future[CreateThreadResponse] =
    validateGrpcRequest(in)
      .map { request =>
        Source
          .single(request)
          .via(createThreadUseCase.execute)
          .via(createThreadPresenter.response)
          .runWith(Sink.head)
      }.valueOr(errorResponse(CreateThreadResponse(false, "", _)))

  override def destroyThread(in: DestroyThreadRequest): Future[DestroyThreadResponse] =
    validateGrpcRequest(in)
      .map { request =>
        Source
          .single(request)
          .via(destroyThreadUseCase.execute)
          .via(destroyThreadPresenter.response)
          .runWith(Sink.head)
      }.valueOr(errorResponse(DestroyThreadResponse(false, "", _)))

  override def joinAdministratorIds(in: JoinAdministratorIdsRequest): Future[JoinAdministratorIdsResponse] =
    validateGrpcRequest(in)
      .map { request =>
        Source
          .single(request)
          .via(joinAdministratorIdsUseCase.execute)
          .via(joinAdministratorIdsPresenter.response)
          .runWith(Sink.head)
      }.valueOr(errorResponse(JoinAdministratorIdsResponse(false, "", _)))

  override def leaveAdministratorIds(in: LeaveAdministratorIdsRequest): Future[LeaveAdministratorIdsResponse] =
    validateGrpcRequest(in)
      .map { request =>
        Source
          .single(request)
          .via(leaveAdministratorIdsUseCase.execute)
          .via(leaveAdministratorIdsPresenter.response)
          .runWith(Sink.head)
      }.valueOr(errorResponse(LeaveAdministratorIdsResponse(false, "", _)))

  override def joinMemberIds(in: JoinMemberIdsRequest): Future[JoinMemberIdsResponse] =
    validateGrpcRequest(in)
      .map { request =>
        Source
          .single(request)
          .via(joinMemberIdsUseCase.execute)
          .via(joinMemberIdsPresenter.response)
          .runWith(Sink.head)
      }.valueOr(errorResponse(JoinMemberIdsResponse(false, "", _)))

  override def leaveMemberIds(in: LeaveMemberIdsRequest): Future[LeaveMemberIdsResponse] =
    validateGrpcRequest(in)
      .map { request =>
        Source
          .single(request)
          .via(leaveMemberIdsUseCase.execute)
          .via(leaveMemberIdsPresenter.response)
          .runWith(Sink.head)
      }.valueOr(errorResponse(LeaveMemberIdsResponse(false, "", _)))

  override def addMessages(in: AddMessagesRequest): Future[AddMessagesResponse] =
    validateGrpcRequest(in)
      .map { request =>
        Source
          .single(request)
          .via(addMessagesUseCase.execute)
          .via(addMessagesPresenter.response)
          .runWith(Sink.head)
      }.valueOr(errorResponse(AddMessagesResponse(false, Seq.empty, _)))

  override def removeMessages(in: RemoveMessagesRequest): Future[RemoveMessagesResponse] =
    validateGrpcRequest(in)
      .map { request =>
        Source
          .single(request)
          .via(removeMessagesUseCase.execute)
          .via(removeMessagesPresenter.response)
          .runWith(Sink.head)
      }.valueOr(errorResponse(RemoveMessagesResponse(false, Seq.empty, _)))

}
