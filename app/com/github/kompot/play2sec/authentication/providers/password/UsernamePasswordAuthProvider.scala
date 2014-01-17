/*
 * Copyright 2012-2014 Joscha Feth, Steve Chaloner, Anton Fedchenko
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
import com.github.kompot.play2sec.authentication.{PlaySecPlugin, MailService}
import com.github.kompot.play2sec.{authentication => atn}
import com.github.kompot.play2sec.authentication.user.NameIdentity
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import com.typesafe.plugin._
import com.github.kompot.play2sec.authentication.providers.AuthProvider
import scala.Some
import play.api.mvc.SimpleResult

abstract class UsernamePasswordAuthProvider[V, UL <: UsernamePasswordAuthUser,
    US <: UsernamePasswordAuthUser, UR <: UsernamePasswordAuthUser,
    L <: (String, String), S<: (String, String), R <: String]
    (implicit app: play.api.Application) extends AuthProvider(app) {
  protected val mailService: MailService

  override protected def requiredSettings = List(
    s"$CFG_MAIL.$CFG_MAIL_DELAY",
    s"$CFG_MAIL.$CFG_MAIL_FROM.$CFG_MAIL_FROM_EMAIL"
  )

  override def onStart() {
    super.onStart()
  }

  override val key = UsernamePasswordAuthProvider.PROVIDER_KEY

  override def authenticate[A](request: Request[A], payload: Option[Case]) =
    payload match {
      case Some(SIGNUP)                => processSignup(request)
      case Some(LOGIN)                 => processLogin(request)
      case Some(RECOVER_PASSWORD)      => processRecover(request)
      case Some(VERIFIED_EMAIL(email)) => processVerifiedEmail(request, email)
      case _ =>
        Future.successful(new LoginSignupResult(com.typesafe.plugin.use[PlaySecPlugin].login.url))
    }

  private def processVerifiedEmail[A](request: Request[A], email: String): Future[LoginSignupResult] = {
    val authUser = buildLoginAuthUser(getVerifiedEmailTuple(email), request)
    Future.successful(new LoginSignupResult(authUser))
  }

  private def processRecover[A](request: Request[A]): Future[LoginSignupResult] = {
    val login: R = getRecover(request)
    val authUser: UR = buildResetPasswordAuthUser(login, request)
    sendResetPasswordEmail(request, authUser)
    Future.successful(
      new LoginSignupResult(use[PlaySecPlugin].passwordRecoveryRequestSuccessful(request))
    )
  }

  private def processLogin[A](request: Request[A]): Future[LoginSignupResult] = {
    import LoginResult._

    val login: L = getLogin(request)
    val authUser: UL = buildLoginAuthUser(login, request)
    Logger.info("processLogin, authUser.expires = " + authUser.expires)
    for {
      r <- loginUser(authUser)
    } yield {
      Logger.info("Login result is " + r)
      r match {
        // The email of the user is not verified
        // should not allow to log in
        case USER_UNVERIFIED =>
          new LoginSignupResult(use[PlaySecPlugin].userLoginUnverified(request))
        // The user exists and the given password was correct
        case USER_LOGGED_IN =>
          new LoginSignupResult(authUser)
        // don't expose this - it might harm users privacy if anyone
        // knows they signed up for our service
        // forward to login page
        case WRONG_PASSWORD | NOT_FOUND =>
          new LoginSignupResult(use[PlaySecPlugin].userNotFound(request))
        // TODO replace with error fallback URL
        case _ =>
          new LoginSignupResult("/")
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
          new LoginSignupResult(use[PlaySecPlugin].userExists(request))
        case USER_EXISTS_UNVERIFIED | USER_CREATED_UNVERIFIED =>
          // User got created as unverified
          // Send validation email
          sendVerifyEmailMailing(request, authUser)
          new LoginSignupResult(use[PlaySecPlugin].userSignupUnverified(request))
        case USER_CREATED =>
          new LoginSignupResult(authUser)
        case _ =>
          // TODO replace with some configurable URL
          new LoginSignupResult("/")
      }
    }
  }

  override val isExternal = false

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
  protected def getVerifiedEmailTuple(email: String): L
  protected def loginUser(authUser: UL): Future[LoginResult.Value]
  protected def signupUser[A](user: US, request: Request[A]): Future[SignupResult.Value]
  protected def generateSignupVerificationRecord(user: US): V
  protected def generateResetPasswordVerificationRecord(user: UR): V
  protected def getVerifyEmailMailingSubject[A](user: US, request: Request[A]): String
  protected def getVerifyEmailMailingBody[A](verificationRecord: V, user: US, request: Request[A]): String
  protected def getResetPasswordEmailMailingSubject[A](user: UR, request: Request[A]): String
  protected def getResetPasswordEmailMailingBody[A](verificationRecord: V, user: UR, request: Request[A]): String
}

object UsernamePasswordAuthProvider {
  val PROVIDER_KEY = "email"
  val CFG_MAIL = "mail"
  val CFG_MAIL_FROM_EMAIL = "email"
  val CFG_MAIL_DELAY = "delay"
  val CFG_MAIL_FROM = "from"

  def handleLogin[A](request: Request[A]): Future[SimpleResult] =
    atn.handleAuthentication(PROVIDER_KEY, request, Some(LOGIN))

  def handleSignup[A](request: Request[A]): Future[SimpleResult] =
    atn.handleAuthentication(PROVIDER_KEY, request, Some(SIGNUP))

  def handleRecoverPassword[A](request: Request[A]): Future[SimpleResult] =
    atn.handleAuthentication(PROVIDER_KEY, request, Some(RECOVER_PASSWORD))

  def handleVerifiedEmailLogin[A](request: Request[A], email: String): Future[SimpleResult] =
    atn.handleAuthentication(PROVIDER_KEY, request, Some(VERIFIED_EMAIL(email)))
}

sealed trait Case

case object SIGNUP extends Case
case object LOGIN extends Case
case object RECOVER_PASSWORD extends Case
case class VERIFIED_EMAIL(email: String) extends Case

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
