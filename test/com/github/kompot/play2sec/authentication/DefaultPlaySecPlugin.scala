/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authentication

import controllers.{JsResponseOk, JsResponseError, JsonWebConversions}
import play.api.mvc._
import com.github.kompot.play2sec.authentication.exceptions.AuthException
import play.api.mvc.Call
import play.api.libs.json.{JsValue, JsString}
import play.api.mvc.Results._
import com.github.kompot.play2sec.authentication.exceptions.AuthException
import play.api.mvc.Call
import play.api.libs.json.JsString
import play.api.mvc.AnyContentAsJson

class DefaultPlaySecPlugin(app: play.api.Application) extends PlaySecPlugin
    with JsonWebConversions {
  def userService = bootstrap.Global.Injector.userStore

  def askMerge = new Call("GET", "/auth/ask-merge")

  def askLink = new Call("GET", "/auth/ask-link")

  def afterLogout[A](a: A) = a match {
    case j: AnyContentAsJson => Ok(JsString("Bye-bye"))
    case _ =>                   Redirect(new Call("GET", "/after-logout"))
  }

  def auth(provider: String) = new Call("GET", s"/auth/external/$provider")

  def login = new Call("GET", "/login")

  def onException(e: AuthException) = new Call("GET", "/onException")

  def afterAuth[A](a: A) = a match {
    case j: AnyContentAsJson => Ok(JsString("Welcome"))
    case _ =>                   Redirect(new Call("GET", "/after-auth"))
  }

  def userExists[A](a: A) = a match {
    case j: AnyContentAsJson => BadRequest[JsValue](JsResponseError("User with such credentials already exists."))
    case _ =>                   Redirect(new Call("GET", "/user-exists"))
  }

  def userSignupUnverified[A](a: A) = a match {
    case j: AnyContentAsJson => Ok[JsValue](JsResponseOk("Email has been sent. You must confirm it."))
    case _ =>                   Redirect(Call("GET", "/auth/user-signup-unverified"))
  }

  def userLoginUnverified[A](a: A) = a match {
    case j: AnyContentAsJson => Ok[JsValue](JsResponseOk("Email was not verified, please check your mail."))
    case _ =>                   Redirect(Call("GET", "/auth/user-login-unverified"))
  }

  def userNotFound[A](a: A) = a match {
    case j: AnyContentAsJson => BadRequest[JsValue](JsResponseError("Unknown email or password."))
    case _ =>                   Redirect(Call("GET", "/auth/user-not-found"))
  }

  def passwordRecoveryRequestSuccessful[A](a: A) = a match {
    case j: AnyContentAsJson => Ok[JsValue](JsResponseError("Please follow instructions in the email."))
    case _ =>                   Redirect(Call("GET", "/auth/password-recovery-request-successful"))
  }
}
