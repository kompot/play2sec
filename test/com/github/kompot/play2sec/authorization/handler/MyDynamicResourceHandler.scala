/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authorization.handler

import collection.immutable.Map
import play.api.mvc.Request
import play.api.data.Form
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.kompot.play2sec.authorization.scala.{DeadboltHandler,
DynamicResourceHandler}
import com.github.kompot.play2sec.authorization.core.models.Subject

class MyDynamicResourceHandler extends DynamicResourceHandler {
  override def isAllowed[A, B](name: String, form: Form[B], handler: DeadboltHandler,
      request: Request[A]) = {
    if (MyDynamicResourceHandler.handlers.get(name) == None) {
      false
    } else if (form.data.isEmpty && form.bindFromRequest()(request).hasErrors) {
      true
    } else {
      MyDynamicResourceHandler.handlers(name).isAllowed(name, form, handler, request)
    }
  }

  override def checkPermission[A](permissionValue: String, deadboltHandler: DeadboltHandler,
      request: Request[A]) = {
    deadboltHandler.getSubject(request).exists(
      _.getPermissions.exists(_.getValue.startsWith(permissionValue)))
  }
}

object MyDynamicResourceHandler {
  val handlers: Map[String, DynamicResourceHandler] = Map(
    UserEditGet.name -> new DynamicResourceHandler {
      override def isAllowed[A, B](name: String, form: Form[B], deadboltHandler: DeadboltHandler, request: Request[A]) = {
        UserEditGet.allowed(
          deadboltHandler.getSubject(request),
          _.exists(isSelfByUsername(getParam(form, request), _))
        )
      }
    },
    UserEditUpdate.name -> new DynamicResourceHandler {
      override def isAllowed[A, B](name: String, form: Form[B], deadboltHandler: DeadboltHandler, request: Request[A]) = {
        UserEditUpdate.allowed(
          deadboltHandler.getSubject(request),
          _.exists(isSelf(getParam(form, request), _))
        )
      }
    },
    UserViewGet.name -> new DynamicResourceHandler {
      override def isAllowed[A, B](name: String, form: Form[B], deadboltHandler: DeadboltHandler, request: Request[A]) = {
        UserViewGet.allowed(
          deadboltHandler.getSubject(request),
          _.exists(isSelfByUsername(getParam(form, request), _))
        )
      }
    }
  )

  private def getParam[B, A](form: Form[B], request: Request[A], idParamName: String = "id"): String = {
    if (form.data.isEmpty) {
      val f1 = form.bindFromRequest()(request)
      f1(idParamName).value.getOrElse(f1.data.toList(0)._2)
    }
    else
      form(idParamName).value.getOrElse(form.data.toList(0)._2)
  }

  private def isSelf(id: String, subj: Subject): Boolean = true

  private def isSelfByUsername(id: String, subj: Subject): Boolean = true
}
