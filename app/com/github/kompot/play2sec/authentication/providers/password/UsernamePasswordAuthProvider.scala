/*
 * Copyright (c) 2013.
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

  override def authenticate[A](request: Request[A], payload: Option[Case.Value]): Any = {
    payload match {
      case Some(Case.SIGNUP) => {
        import SignupResult._
        val signup: S = getSignup(request)
        val authUser: US = buildSignupAuthUser(signup, request)
        val r = signupUser(authUser, request)
        Logger.debug("Signup result is " + r)
        val as: Any = r match {
          case USER_EXISTS =>
            // TODO: more elegant way to check this?
            if (request.body.isInstanceOf[AnyContentAsJson])
              userExistsJson(authUser)
            else
              userExists(authUser).url
          case USER_EXISTS_UNVERIFIED | USER_CREATED_UNVERIFIED => {
            // User got created as unverified
            // Send validation email
            sendVerifyEmailMailing(request, authUser)
            // TODO: more elegant way to check this?
            if (request.body.isInstanceOf[AnyContentAsJson])
              userSignupUnverifiedJson(authUser)
            else
              userUnverified(authUser).url
          }
          case USER_CREATED => authUser
          case _ => throw new AuthException("Something in signup went wrong")
        }
        Logger.debug("Signup result is " + as)
        as
      }
      case Some(Case.LOGIN) => {
        import LoginResult._

        val login: L = getLogin(request)
        val authUser: UL = buildLoginAuthUser(login, request)
        val r = loginUser(authUser)
        r match {
          // The email of the user is not verified, yet - we won't allow him to log in
          case USER_UNVERIFIED => {
            if (request.body.isInstanceOf[AnyContentAsJson]) {
              userLoginUnverifiedJson(authUser)
            } else {
              userUnverified(authUser).url
            }
          }
          // The user exists and the given password was correct
          case USER_LOGGED_IN => authUser
          // don't expose this - it might harm users privacy if anyone
          // knows they signed up for our service
          // forward to login page
          case WRONG_PASSWORD | NOT_FOUND => {
            // TODO: more elegant way to check this?
            if (request.body.isInstanceOf[AnyContentAsJson]) {
              onLoginUserNotFoundJson(request)
            } else {
              onLoginUserNotFound(request)
            }
          }
          case _ => throw new AuthException("Something in login went wrong")
        }
      }
      case Some(Case.RECOVER_PASSWORD) => {
        val login: R = getRecover(request)
        val authUser: UR = buildResetPasswordAuthUser(login, request)
        sendResetPasswordEmail(request, authUser)
        if (request.body.isInstanceOf[AnyContentAsJson]) {
          onSuccessfulRecoverPasswordJson()
        } else {
          // TODO where to redirect?
//          userUnverified(authUser).url
        }
      }
      case _ => {
        com.typesafe.plugin.use[PlaySecPlugin].login.url
      }
    }
  }

  override def isExternal = false

  // TODO used?
//  override def getSessionAuthUser(id: String, expires: Long): AuthUser

  protected def onLoginUserNotFound[A](request: Request[A]): String = com.typesafe.plugin.use[PlaySecPlugin].login.url

  protected def onLoginUserNotFoundJson[A](request: Request[A]): SimpleResult[JsValue]

  protected def onSuccessfulRecoverPasswordJson[A](): SimpleResult[JsValue]

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
    mailService.sendMail(subject, Array(getEmailName(user.getEmail, "")), body)
  }

  def getEmailName(user: US): String = {
    val name = if (user.isInstanceOf[NameIdentity]) user.asInstanceOf[NameIdentity].getName else ""
    getEmailName(user.getEmail, name)
  }

  def getEmailName(email: String, name: String): String = mailService.getEmailName(email, name)

  protected def buildLoginAuthUser[A](login: L, request: Request[A]): UL
  protected def buildSignupAuthUser[A](signup: S, request: Request[A]): US
  protected def buildResetPasswordAuthUser[A](recover: R, request: Request[A]): UR
  protected def getLoginForm: Form[L]
  protected def getSignupForm: Form[S]
  protected def getResetPasswordForm: Form[R]
  protected def loginUser(authUser: UL): LoginResult.Value
  protected def signupUser[A](user: US, request: Request[A]): SignupResult.Value
  protected def userExists(authUser: UsernamePasswordAuthUser): Call
  protected def userExistsJson(authUser: UsernamePasswordAuthUser): SimpleResult[JsValue]
  protected def userUnverified(authUser: UsernamePasswordAuthUser): Call
  protected def userSignupUnverifiedJson(user: US): SimpleResult[JsValue]
  protected def userLoginUnverifiedJson(value: UL): SimpleResult[JsValue]
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

  def handleLogin[A](request: Request[A]): Result =
    atn.handleAuthentication(PROVIDER_KEY, request, Some(Case.LOGIN))
  def handleSignup[A](request: Request[A]): Result =
    atn.handleAuthentication(PROVIDER_KEY, request, Some(Case.SIGNUP))
  def handleRecoverPassword[A](request: Request[A]): Result =
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
}

object LoginResult extends Enumeration {
  val USER_UNVERIFIED = Value
  val USER_LOGGED_IN = Value
  val NOT_FOUND = Value
  val WRONG_PASSWORD = Value
}

// TODO: Used?
trait UsernamePassword {
  def getEmail: String
  def getPassword: String
}
