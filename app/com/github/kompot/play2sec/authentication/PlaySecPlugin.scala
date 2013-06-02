/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication

import play.api.Plugin
import play.api.libs.json.JsValue
import play.api.mvc.{SimpleResult, Call}
import com.github.kompot.play2sec.authentication.exceptions.AuthException
import com.github.kompot.play2sec.authentication.service.UserService
import com.github.kompot.play2sec.authentication.user.AuthUser

trait PlaySecPlugin extends Plugin {
  def afterAuth: Call

  def askMerge: Call

  def askLink: Call

  def afterLogout: Call

  def auth(provider: String): Call

  def login: Call

  def onException(e: AuthException): Call

  def userService: UserService

  def afterAuthJson(loginUser: AuthUser): SimpleResult[JsValue]
}
