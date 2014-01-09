/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authentication

import controllers.JsonWebConversions
import play.api.mvc._
import com.github.kompot.play2sec.authentication.exceptions.AuthException
import play.api.mvc.Call
import play.api.libs.json.JsString

class DefaultPlaySecPlugin(app: play.api.Application) extends PlaySecPlugin
    with JsonWebConversions {
  def userService = bootstrap.Global.Injector.userStore

  def askMerge = new Call("GET", "/auth/ask-merge")

  def askLink = new Call("GET", "/auth/ask-link")

  def afterLogout[A](a: A) = a match {
    case AnyContentAsJson(_) => Results.Ok(JsString("Bye-bye"))
    case _ =>                   Results.Redirect(new Call("GET", "/after-logout"))
  }

  def auth(provider: String) = new Call("GET", s"/auth/external/$provider")

  def login = new Call("GET", "/login")

  def onException(e: AuthException) = new Call("GET", "/onException")

  def afterAuth[A](a: A) = a match {
    case AnyContentAsJson(_) => Results.Ok(JsString("Welcome"))
    case _ =>                   Results.Redirect(new Call("GET", "/after-auth"))
  }
}
