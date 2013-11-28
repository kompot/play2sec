/*
 * Copyright 2012-2013 Joscha Feth, Steve Chaloner, Anton Fedchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.kompot.play2sec.authentication.providers.password

import play.api.mvc._
import play.api.data.Form
import UsernamePasswordAuthProvider._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.Call
import play.api.mvc.AnyContentAsJson
import com.github.kompot.play2sec.authentication.{PlaySecPlugin, MailService}
import com.github.kompot.play2sec.authentication.providers.AuthProvider
import com.github.kompot.play2sec.{authentication => atn}
import com.github.kompot.play2sec.authentication.user.NameIdentity
import com.github.kompot.play2sec.authentication.exceptions.AuthException
import scala.concurrent.{Promise, Await, ExecutionContext, Future}
import ExecutionContext.Implicits.global

abstract class UsernamePasswordAuthProvider[V, UL <: UsernamePasswordAuthUser,
    US <: UsernamePasswordAuthUser, UR <: UsernamePasswordAuthUser,
    L <: (String, String), S<: (String, String), R <: String]
    (implicit app: play.api.Application) extends AuthProvider(app) {
  protected val mailService: MailService

  override protected def neededSettingKeys = List(
    s"$SETTING_KEY_MAIL.$SETTING_KEY_MAIL_DELAY",
    s"$SETTING_KEY_MAIL.$SETTING_KEY_MAIL_FROM.$SETTING_KEY_MAIL_FROM_EMAIL"
  )

  override def onStart() {
    super.onStart()
  }

  override def getKey = UsernamePasswordAuthProvider.PROVIDER_KEY

  override def authenticate[A](request: Request[A], payload: Option[Case.Value]) =
    payload match {
      case Some(Case.SIGNUP)           => processSignup(request)
      case Some(Case.LOGIN)            => processLogin(request)
      case Some(Case.RECOVER_PASSWORD) => processRecover(request)
      case _ => {
        Future.successful(new LoginSignupResult(com.typesafe.plugin.use[PlaySecPlugin].login.url))
      }
    }


  private def processRecover[A](request: Request[A]): Future[LoginSignupResult] = {
    val login: R = getRecover(request)
    val authUser: UR = buildResetPasswordAuthUser(login, request)
    sendResetPasswordEmail(request, authUser)
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      Future.successful(new LoginSignupResult(onSuccessfulRecoverPasswordJson()))
    } else {
      // TODO where to redirect?
      // userUnverified(authUser).url
      Future.successful(new LoginSignupResult("/"))
    }
  }

  private def processLogin[A](request: Request[A]): Future[LoginSignupResult] = {
    import LoginResult._

    val login: L = getLogin(request)
    val authUser: UL = buildLoginAuthUser(login, request)
    Logger.warn("processLogin 1")
    for {
      r <- loginUser(authUser)
    } yield {
      Logger.info("Login result is " + r)
      r match {
        // The email of the user is not verified, yet -
        // we won't allow him to log in
        case USER_UNVERIFIED => {
          if (request.body.isInstanceOf[AnyContentAsJson]) {
            new LoginSignupResult(userLoginUnverifiedJson(authUser))
          } else {
            new LoginSignupResult(userUnverified(authUser).url)
          }
        }
        // The user exists and the given password was correct
        case USER_LOGGED_IN => new LoginSignupResult(authUser)
        // don't expose this - it might harm users privacy if anyone
        // knows they signed up for our service
        // forward to login page
        case WRONG_PASSWORD | NOT_FOUND => {
          // TODO: more elegant way to check this?
          if (request.body.isInstanceOf[AnyContentAsJson]) {
            new LoginSignupResult(onLoginUserNotFoundJson(request))
          } else {
            new LoginSignupResult(onLoginUserNotFound(request))
          }
        }
        // TODO replace with error fallback URL
        case _ => new LoginSignupResult("/")
      }
    }
  }

  private def processSignup[A](request: Request[A]): Future[LoginSignupResult] = {
    import SignupResult._
    val signup: S = getSignup(request)
    val authUser: US = buildSignupAuthUser(signup, request)
    for {
      r <- signupUser(authUser, request)
    } yield {
      Logger.info("Signup result is " + r)
      r match {
        case USER_EXISTS =>
          // TODO: more elegant way to check this?
          if (request.body.isInstanceOf[AnyContentAsJson]) {
            new LoginSignupResult(userExistsJson(authUser))
          } else {
            new LoginSignupResult(userExists(authUser).url)
          }
        case USER_EXISTS_UNVERIFIED | USER_CREATED_UNVERIFIED => {
          // User got created as unverified
          // Send validation email
          sendVerifyEmailMailing(request, authUser)
          // TODO: more elegant way to check this?
          if (request.body.isInstanceOf[AnyContentAsJson]) {
            new LoginSignupResult(userSignupUnverifiedJson(authUser))
          } else {
            new LoginSignupResult(userUnverified(authUser).url)
          }
        }
        case USER_CREATED => new LoginSignupResult(authUser)
        // TODO replace with error fallback URL
        case _ => new LoginSignupResult("/")
      }
    }
  }

  override def isExternal = false

  // TODO used?
//  override def getSessionAuthUser(id: String, expires: Long): AuthUser

  protected def onLoginUserNotFound[A](request: Request[A]): String = com.typesafe.plugin.use[PlaySecPlugin].login.url

  protected def onLoginUserNotFoundJson[A](request: Request[A]): SimpleResult

  protected def onSuccessfulRecoverPasswordJson[A](): SimpleResult

  private def getSignup[A](request: Request[A]): S = getSignupForm.bindFromRequest()(request).get

  private def getLogin[A](request: Request[A]): L = getLoginForm.bindFromRequest()(request).get

  private def getRecover[A](request: Request[A]): R = getResetPasswordForm.bindFromRequest()(request).get

  private def sendVerifyEmailMailing[A](request: Request[A], user: US)  {
    val subject = getVerifyEmailMailingSubject(user, request)
    val record = generateSignupVerificationRecord(user)
    val body = getVerifyEmailMailingBody(record, user, request)
    mailService.sendMail(subject, Array(getEmailName(user)), body)
  }

  private def sendResetPasswordEmail[A](request: Request[A], user: UR)  {
    val subject = getResetPasswordEmailMailingSubject(user, request)
    val record = generateResetPasswordVerificationRecord(user)
    val body = getResetPasswordEmailMailingBody(record, user, request)
    mailService.sendMail(subject, Array(getEmailName(user.email, "")), body)
  }

  def getEmailName(user: US): String = {
    val name = user match {
      case identity: NameIdentity => identity.name
      case _ => ""
    }
    getEmailName(user.email, name)
  }

  def getEmailName(email: String, name: String): String = mailService.getEmailName(email, name)

  protected def buildLoginAuthUser[A](login: L, request: Request[A]): UL
  protected def buildSignupAuthUser[A](signup: S, request: Request[A]): US
  protected def buildResetPasswordAuthUser[A](recover: R, request: Request[A]): UR
  protected def getLoginForm: Form[L]
  protected def getSignupForm: Form[S]
  protected def getResetPasswordForm: Form[R]
  protected def loginUser(authUser: UL): Future[LoginResult.Value]
  protected def signupUser[A](user: US, request: Request[A]): Future[SignupResult.Value]
  protected def userExists(authUser: UsernamePasswordAuthUser): Call
  protected def userExistsJson(authUser: UsernamePasswordAuthUser): SimpleResult
  protected def userUnverified(authUser: UsernamePasswordAuthUser): Call
  protected def userSignupUnverifiedJson(user: US): SimpleResult
  protected def userLoginUnverifiedJson(value: UL): SimpleResult
  protected def generateSignupVerificationRecord(user: US): V
  protected def generateResetPasswordVerificationRecord(user: UR): V
  protected def getVerifyEmailMailingSubject[A](user: US, request: Request[A]): String
  protected def getVerifyEmailMailingBody[A](verificationRecord: V, user: US, request: Request[A]): String
  protected def getResetPasswordEmailMailingSubject[A](user: UR, request: Request[A]): String
  protected def getResetPasswordEmailMailingBody[A](verificationRecord: V, user: UR, request: Request[A]): String
}

object UsernamePasswordAuthProvider {
  val PROVIDER_KEY = "email"
  val SETTING_KEY_MAIL = "mail"
  val SETTING_KEY_MAIL_FROM_EMAIL = "email"
  val SETTING_KEY_MAIL_DELAY = "delay"
  val SETTING_KEY_MAIL_FROM = "from"

  def handleLogin[A](request: Request[A]): Future[SimpleResult] =
    atn.handleAuthentication(PROVIDER_KEY, request, Some(Case.LOGIN))

  def handleSignup[A](request: Request[A]): Future[SimpleResult] =
    atn.handleAuthentication(PROVIDER_KEY, request, Some(Case.SIGNUP))

  def handleRecoverPassword[A](request: Request[A]): Future[SimpleResult] =
    atn.handleAuthentication(PROVIDER_KEY, request, Some(Case.RECOVER_PASSWORD))
}

object Case extends Enumeration {
  val SIGNUP = Value
  val LOGIN = Value
  val RECOVER_PASSWORD = Value
}

object SignupResult extends Enumeration {
  val USER_EXISTS = Value
  val USER_CREATED_UNVERIFIED = Value
  val USER_CREATED = Value
  val USER_EXISTS_UNVERIFIED = Value
  val UNKNOWN_ERROR = Value
}

object LoginResult extends Enumeration {
  val USER_UNVERIFIED = Value
  val USER_LOGGED_IN = Value
  val NOT_FOUND = Value
  val WRONG_PASSWORD = Value
  val UNKNOWN_ERROR = Value
}

// TODO: Used?
trait UsernamePassword {
  def getEmail: String
  def getPassword: String
}
