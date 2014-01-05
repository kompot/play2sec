/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authentication.providers.anonymous

import play.api.mvc.Request
import com.github.kompot.play2sec.authentication.PlaySecPlugin
import com.github.kompot.play2sec.authentication.providers.AuthProvider
import com.github.kompot.play2sec.authentication.providers.password
.{LoginSignupResult, Case}
import com.github.kompot.play2sec.authentication
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import mock.KvStore

class AnonymousAuthProvider(implicit app: play.api.Application) extends AuthProvider(app) {
  def getKey = "anonymous"

  def authenticate[A](request: Request[A], payload: Option[Case.Value]) = {
    payload match {
      case Case.SIGNUP => {
        val user = new AnonymousAuthUser(KvStore.generateId)
        for {
          u <- authentication.getUserService.save(user)
        } yield {
          new LoginSignupResult(user)
        }
      }
      case _ => Future.successful(new LoginSignupResult(com.typesafe.plugin.use[PlaySecPlugin].login.url))
    }
  }

  protected def requiredSettings = List.empty

  override val isExternal = false
}
