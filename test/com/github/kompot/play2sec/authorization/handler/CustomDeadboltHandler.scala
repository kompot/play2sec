/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authorization.handler

import play.api.mvc.{SimpleResult, Request, Results}
import com.github.kompot.play2sec.authorization.scala._
import com.github.kompot.play2sec.authorization.core.models.Subject
import com.github.kompot.play2sec.authentication
import scala.concurrent.Future
import model.MongoWait

class CustomDeadboltHandler(dynamicResourceHandler: Option[DynamicResourceHandler] = None) extends DeadboltHandler {
  val userService = bootstrap.Global.Injector.userService

  def beforeAuthCheck[A](request: Request[A]) = None

  override def getDynamicResourceHandler[A](request: Request[A]): Option[DynamicResourceHandler] = {
    if (dynamicResourceHandler.isDefined)
      dynamicResourceHandler
    else
      Some(new MyDynamicResourceHandler())
  }

  override def getSubject[A](request: Request[A]): Option[Subject] =
    authentication.getUser(request).flatMap(u =>
      MongoWait(userService.getByAuthUserIdentity(u)))

  def onAuthFailure[A](request: Request[A]): Future[SimpleResult] = {
    Future.successful(Results.Forbidden("Access denied."))
  }
}
