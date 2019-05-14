package com.github.j5ik2o.threadWeaver.adaptor.grpc.service

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.typed.scaladsl.ActorMaterializer
import cats.data.NonEmptyList
import com.github.j5ik2o.threadWeaver.adaptor.error.InterfaceError
import com.github.j5ik2o.threadWeaver.adaptor.grpc.model._
import com.github.j5ik2o.threadWeaver.adaptor.grpc.presenter.CreateThreadPresenter
import com.github.j5ik2o.threadWeaver.adaptor.grpc.validator.ThreadValidatorSupport
import com.github.j5ik2o.threadWeaver.adaptor.grpc.validator.ThreadValidatorSupport._
import com.github.j5ik2o.threadWeaver.adaptor.http.presenter.JoinAdministratorIdsPresenter
import com.github.j5ik2o.threadWeaver.useCase.{ CreateThreadUseCase, JoinAdministratorIdsUseCase }
import wvlet.airframe.bind

import scala.concurrent.Future

trait ThreadCommandServiceImpl extends ThreadCommandService with ThreadValidatorSupport {

  private implicit val system       = bind[ActorSystem[Nothing]]
  private val createThreadUseCase   = bind[CreateThreadUseCase]
  private val createThreadPresenter = bind[CreateThreadPresenter]

  private val joinAdministratorIdsUseCase   = bind[JoinAdministratorIdsUseCase]
  private val joinAdministratorIdsPresenter = bind[JoinAdministratorIdsPresenter]

  implicit val mat = ActorMaterializer()

  private val errorResponse = { f: NonEmptyList[InterfaceError] =>
    Future.successful(CreateThreadResponse(isSuccessful = false, "", f.map(_.message).toList))
  }

  override def createThread(in: CreateThreadRequest): Future[CreateThreadResponse] = {
    validateGrpcRequest(in)
      .map { request =>
        Source
          .single(request)
          .via(createThreadUseCase.execute)
          .via(createThreadPresenter.response)
          .runWith(Sink.head)
      }.valueOr(errorResponse)
  }

  override def destroyThread(in: DestroyThreadRequest): Future[DestroyThreadResponse] = { ??? }

  override def joinAdministratorIds(in: JoinAdministratorIdsRequest): Future[JoinAdministratorIdsResponse] = ???

  override def leaveAdministratorIds(in: LeaveAdministratorIdsRequest): Future[LeaveAdministratorIdsResponse] = ???

  override def joinMemberIds(in: JoinAdministratorIdsRequest): Future[JoinAdministratorIdsResponse] = ???

  override def leaveMemberIds(in: LeaveAdministratorIdsRequest): Future[LeaveAdministratorIdsResponse] = ???

  override def addMessages(in: AddMessagesRequest): Future[AddMessagesResponse] = ???

  override def removeMessages(in: RemoveMessagesRequest): Future[RemoveMessagesResponse] = ???

}
