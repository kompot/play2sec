/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authentication

import com.github.kompot.play2sec.authentication.exceptions.AuthException
import com.github.kompot.play2sec.authentication.user.AuthUser
import controllers.{JsonWebConversions, JsResponseOk}
import play.api.libs.json.{Json, JsString, JsValue}
import play.api.mvc.{Results, Call}

class DefaultPlaySecPlugin(app: play.api.Application) extends PlaySecPlugin
    with JsonWebConversions {
  def afterAuth = new Call("GET", "/")

  def askMerge = new Call("GET", "/auth/ask-merge")

  def askLink = new Call("GET", "/auth/ask-link")

  def afterLogout = new Call("GET", "/after-logout")

  def auth(provider: String) = new Call("GET", s"/auth/external/$provider")

  def login = new Call("GET", "/login")

  def onException(e: AuthException) = new Call("GET", "/onException")

  def userService = bootstrap.Global.Injector.userService

  def afterAuthJson(loginUser: AuthUser) = Results.Ok[JsValue](
    JsResponseOk("Welcome to Example.com.", Json.obj(
      "authUser" -> Json.obj("id" -> JsString(loginUser.id))
    ))
  )
}
