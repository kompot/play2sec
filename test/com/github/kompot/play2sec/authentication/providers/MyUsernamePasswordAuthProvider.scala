/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authentication.providers

import com.github.kompot.play2sec.authentication.providers.password
.{UsernamePasswordAuthUser, SignupResult, LoginResult,
UsernamePasswordAuthProvider}
import controllers._
import model._
import play.api.Logger
import play.api.Play.current
import play.api.libs.json.{Json, JsObject, JsString, JsValue}
import play.api.mvc.{Call, Results, Request}
import play.api.templates.Html
import play.i18n.Messages
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import com.github.kompot.play2sec.authentication
import controllers.JsResponseError
import controllers.JsResponseOk
import model.User
import model.Token
import controllers.JsResponseError
import bootstrap.Global.Injector
import ExecutionContext.Implicits.global
import Results._

class MyUsernamePasswordAuthProvider(app: play.Application)
    extends UsernamePasswordAuthProvider[Token, MyLoginUsernamePasswordAuthUser,
        MyUsernamePasswordAuthUser, MyUsernamePasswordAuthUser, (String, String),
        (String, String), String] with JsonWebConversions {
  lazy val tokenService = bootstrap.Global.Injector.tokenStore
  protected val mailService = bootstrap.Global.Injector.mailService
  lazy val userService = bootstrap.Global.Injector.userStore

  override protected def requiredSettings = {
    super.requiredSettings ++ List(
      MyUsernamePasswordAuthProvider.SETTING_KEY_VERIFICATION_LINK_SECURE,
      MyUsernamePasswordAuthProvider.SETTING_KEY_PASSWORD_RESET_LINK_SECURE,
      MyUsernamePasswordAuthProvider.SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET
    )
  }

  protected def buildLoginAuthUser[A](login: (String, String), request: Request[A]) = new MyLoginUsernamePasswordAuthUser(login._2, login._1)

  protected def buildSignupAuthUser[A](signup: (String, String), request: Request[A]) = new MyUsernamePasswordAuthUser(signup._2, signup._1)

  protected def buildResetPasswordAuthUser[A](resetPassword: String, request: Request[A]) = new MyUsernamePasswordAuthUser("", resetPassword)

  protected def getLoginForm = Authorization.loginForm

  protected def getSignupForm = Authorization.signupForm

  protected def getResetPasswordForm = Authorization.resetPasswordForm

  protected def loginUser(authUser: MyLoginUsernamePasswordAuthUser): Future[LoginResult.Value] = {
    for {
      u <- userService.getByAuthUserIdentity(authUser)
    } yield {
      if (!u.isDefined) {
        LoginResult.NOT_FOUND
      } else {
        if (!u.get.emailValidated) {
          LoginResult.USER_UNVERIFIED
        } else {
          val goodPassword = u.get.remoteUsers.exists(_.provider == getKey &&
              authUser.checkPassword(u.get.password, authUser.clearPassword))
          if (goodPassword)
            LoginResult.USER_LOGGED_IN
          else
            LoginResult.WRONG_PASSWORD
        }
      }
    }
  }

  protected def signupUser[A](user: MyUsernamePasswordAuthUser, request: Request[A]): Future[SignupResult.Value] = {
    for {
      u <- userService.getByAuthUserIdentity(user)
      saved <- if (!u.isDefined) authentication.getUserService.save(user) else Future.successful(None)
    } yield {
      if (u.isDefined) {
        if (u.get.emailValidated) {
          // This user exists, has its email validated and is active
          SignupResult.USER_EXISTS
        } else {
          // this user exists, is active but has not yet validated its
          // email
          SignupResult.USER_EXISTS_UNVERIFIED
        }
      } else {
        // Usually the email should be verified before allowing login, however
        // if you return SignupResult.USER_CREATED then the user gets logged in directly
        SignupResult.USER_CREATED_UNVERIFIED
      }
    }
  }

  protected def userExists(authUser: UsernamePasswordAuthUser) = Call("GET", "/auth/user-exists")

  protected def userUnverified(authUser: UsernamePasswordAuthUser) = Call("GET", "/auth/user-unverified")

  protected def generateSignupVerificationRecord(user: MyUsernamePasswordAuthUser) = {
    Await(tokenService.generateToken(user, Json.obj("email" -> JsString(user.email))))
  }

  protected def generateResetPasswordVerificationRecord(user: MyUsernamePasswordAuthUser) =
    ???

  protected def getVerifyEmailMailingSubject[A](user: MyUsernamePasswordAuthUser, request: Request[A]) =
    "play2sec: please verify your email address"

  protected def getVerifyEmailMailingBody[A](vr: Token, user: MyUsernamePasswordAuthUser, request: Request[A]) = {
    s"Please confirm ${user.email} by clicking on ${getVerificationLink(vr, user, request)}"
//    views.html.mail.verifySignup(user.email, getVerificationLink(vr, user, request)).body
  }

  protected def getResetPasswordEmailMailingSubject[A](user: MyUsernamePasswordAuthUser, request: Request[A]) =
    Messages.get("mail.subject.resetPassword")

  protected def getResetPasswordEmailMailingBody[A](vr: Token, user: MyUsernamePasswordAuthUser, request: Request[A]) = {
//    views.html.mail.passwordReset(Html(getResetPasswordLink(vr, user, request)))(request).body
    // TODO
    "getResetPasswordEmailMailingBody"
  }

  private def getVerificationLink[A](token: Token, user: MyUsernamePasswordAuthUser,
      request: Request[A]): String = {
    Call("GET", "/auth/verify-email?token=" + token.securityKey).absoluteURL()(request)
//    routes.Authorization.verifyEmailAndLogin(token.securityKey).absoluteURL()(request)
  }

//  private def getResetPasswordLink[A](verificationRecord: Token, user: MyUsernamePasswordAuthUser, request: Request[A]): String = {
//    routes.Authorization.resetPassword(verificationRecord.securityKey).absoluteURL()(request)
//  }

  protected def userExistsJson(authUser: UsernamePasswordAuthUser) =
    BadRequest[JsValue](JsResponseError(
      "User with such credentials already exists."))

  def userSignupUnverifiedJson(user: MyUsernamePasswordAuthUser) =
    Ok[JsValue](
      JsResponseOk("Email has been sent. You must confirm it.")
    )

  def userLoginUnverifiedJson(value: MyLoginUsernamePasswordAuthUser) =
    BadRequest[JsValue](JsResponseError("Email was not verified, please check your mail."))

  protected def onLoginUserNotFoundJson[A](request: Request[A]) =
    BadRequest[JsValue](JsResponseError("Unknown email or password."))

  protected def onSuccessfulRecoverPasswordJson[A]() =
    Ok[JsValue](JsResponseOk(
      "Please follow instructions in the email."))
}

object MyUsernamePasswordAuthProvider {
  val SETTING_KEY_VERIFICATION_LINK_SECURE = UsernamePasswordAuthProvider.SETTING_KEY_MAIL + "." + "verificationLink.secure"
  val SETTING_KEY_PASSWORD_RESET_LINK_SECURE = UsernamePasswordAuthProvider.SETTING_KEY_MAIL + "." + "passwordResetLink.secure"
  val SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET = "loginAfterPasswordReset"
  // TODO: used?
//  val getProvider = a.getProvider(UsernamePasswordAuthProvider.PROVIDER_KEY).get.asInstanceOf[MyUsernamePasswordAuthProvider]
}
