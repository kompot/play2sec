package com.github.kompot.play2sec

import bootstrap.Global.Injector
import com.github.kompot.play2sec.authentication
import com.github.kompot.play2sec.authentication.providers.password.{Case,
UsernamePasswordAuthProvider}
import com.github.kompot.play2sec.authentication.user.AuthUser
import controllers.{JsonWebConversions, JsResponseError, Authorization}
import model.Await
import play.api.libs.json.JsValue
import play.api.mvc.{Handler, Results, Action}
import play.api.templates.Html
import play.api.test.FakeApplication
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FakeApp extends FakeApplication(
  withRoutes = FakeApp.routes,
  additionalConfiguration = FakeApp.additionalConfiguration,
  additionalPlugins = FakeApp.additionalPlugins ++ FakeApp.normalUser)

class FakeAppShortLivingUser extends FakeApplication(
  withRoutes = FakeApp.routes,
  additionalConfiguration = FakeApp.additionalConfiguration,
  additionalPlugins = FakeApp.additionalPlugins ++ FakeApp.shortLivingUser)

class FakeAppNoAutoLink extends FakeApplication(
  withRoutes = FakeApp.routes,
  additionalConfiguration = FakeApp.additionalConfiguration ++ FakeApp.noAutoLink,
  additionalPlugins = FakeApp.additionalPlugins ++ FakeApp.normalUser)

class FakeAppNoAutoMerge extends FakeApplication(
  withRoutes = FakeApp.routes,
  additionalConfiguration = FakeApp.additionalConfiguration ++ FakeApp.noAutoMerge,
  additionalPlugins = FakeApp.additionalPlugins ++ FakeApp.normalUser)

object FakeApp extends JsonWebConversions {
  val routes: PartialFunction[(String, String), Handler] = {
    case ("GET", path: String) if path.startsWith("/auth/external/") =>
      Action.async { implicit request =>
        authentication.handleAuthentication(path.substring("/auth/external/".length), request)
      }
    case ("POST", "/auth/signup") =>
      Action.async { implicit request =>
        Authorization.userSignUpForm.bindFromRequest.fold(
          { errors => Future.successful(Results.BadRequest[JsValue](
            JsResponseError("Email signup failed.", Some(errors)))) },
          { case _ => UsernamePasswordAuthProvider.handleSignup(request) }
        )
      }
    case ("POST", "/auth/login") =>
      Action.async { implicit request =>
        Authorization.userLoginForm.bindFromRequest.fold(
          { errors => Future.successful(Results.BadRequest[JsValue](
            JsResponseError("Email login failed.", Some(errors)))) },
          { case _ => UsernamePasswordAuthProvider.handleLogin(request) }
        )
      }
    case ("GET", "/auth/signup") | ("GET", "/auth/login") =>
      Action { implicit request =>
        Results.Ok(
          Html("""
            <form method="post">
              <input type="text"     name="email"    id="email" />
              <input type="password" name="password" id="password" />
              <input type="submit" />
            </form>
               """))
      }
    case ("GET", "/auth/verify-email") =>
      Action.async { implicit request =>
        for {
          maybeToken <- Injector.tokenStore.getValidTokenBySecurityKey(request.getQueryString("token").get)
          email = maybeToken.get.data.\("email").as[String]
          res <- Injector.userStore.verifyEmail(maybeToken.get.userId, email)
        } yield {
          if (res) {
            Await(UsernamePasswordAuthProvider.handleVerifiedEmailLogin(request, email))
          } else {
            Results.BadRequest("Unable to process request.")
          }
        }
      }
    case ("GET", "/auth/logout") =>
      Action { implicit request =>
        authentication.logout(request)
      }
    case ("GET", "/auth/ask-link") =>
      Action { implicit request =>
        Results.Ok(Html(
          """
            <p>You did not use these credentials to log in into your account before.</p>
            <p>Would you like to link this method to your account so that you are able to login using all of them?</p>
            <p>If you don't check the box then a separate account will be created.</p>
            <form action="/auth/link" method="post">
              <input type="checkbox" name="link" id="link" value="true" /> Link account<br />
              <input type="submit" />
            </form>
          """.stripMargin))
      }
    case ("POST", "/auth/link") =>
      Action.async { implicit request =>
        Authorization.linkForm.bindFromRequest.fold(
          { errors => Future.successful(Results.BadRequest[JsValue](
            JsResponseError("Link form is invalid.", Some(errors)))) },
          { case formLinkValue => authentication.link(request, formLinkValue)
          }
        )
      }
    case ("GET", "/auth/ask-merge") =>
      Action { implicit request =>
        Results.Ok(Html(
          """
            <p>We've found that account you just used is linked to some other account in our system.</p>
            <p>Would you like to merge them?</p>
            <p>If you don't check the box then you will logged in with that account and no actions will be performed.</p>
            <form action="/auth/merge" method="post">
              <input type="checkbox" name="merge" id="merge" value="true" /> Merge accounts<br />
              <input type="submit" />
            </form>
          """.stripMargin))
      }
    case ("POST", "/auth/merge") =>
      Action.async { implicit request =>
        Authorization.mergeForm.bindFromRequest.fold(
          { errors => Future.successful(Results.BadRequest[JsValue](
            JsResponseError("Merge form is invalid.", Some(errors)))) },
          { case formMergeValue => authentication.merge(request, formMergeValue)
          }
        )
      }
    case ("GET", "/") => Action(Results.Ok("It's home baby"))
  }
  val additionalConfiguration = Map(
    "application.secret" -> "123",
    "application.global" -> "bootstrap.Global",
    "play2sec.facebook.authorizationUrl" -> "https://graph.facebook.com/oauth/authorize",
    "play2sec.facebook.accessTokenUrl" -> "https://graph.facebook.com/oauth/access_token",
    "play2sec.facebook.userInfoUrl" -> "https://graph.facebook.com/me",
    "play2sec.facebook.scope" -> "email",
    "play2sec.twitter.requestTokenUrl" -> "https://api.twitter.com/oauth/request_token",
    "play2sec.twitter.accessTokenUrl" -> "https://api.twitter.com/oauth/access_token",
    "play2sec.twitter.authorizationUrl" -> "https://api.twitter.com/oauth/authorize",
    "play2sec.twitter.userInfoUrl" -> "https://api.twitter.com/1.1/account/verify_credentials.json",
    "play2sec.twitter.redirectUri.secure" -> "false",
    "play2sec.google.authorizationUrl" -> "https://accounts.google.com/o/oauth2/auth",
    "play2sec.google.accessTokenUrl" -> "https://accounts.google.com/o/oauth2/token",
    "play2sec.google.userInfoUrl" -> "https://www.googleapis.com/oauth2/v1/userinfo",
    "play2sec.google.scope" -> "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email",
    "play2sec.accountAutoLink" -> "true",
    "play2sec.accountMergeEnabled" -> "true",
    "play2sec.accountAutoMerge" -> "true",
    "play2sec-mail.from.email" -> "play2sec@kompot.name",
    "play2sec-mail.from.name" -> "play2sec",
    "play2sec-mail.from.delay" -> "1",
    "play2sec.email.mail.verificationLink.secure" -> "false",
    "play2sec.email.mail.passwordResetLink.secure" -> "false",
    "play2sec.email.mail.from.email" -> "play2sec@kompot.name",
    "play2sec.email.mail.from.name" -> "play2sec@kompot.name",
    "play2sec.email.mail.delay" -> "1",
    "play2sec.email.loginAfterPasswordReset" -> "true",
    "smtp.host" -> "smtp.gmail.com",
    "smtp.port" -> "587",
    "smtp.tls" -> "true",
    "smtp.user" -> "kompotik@gmail.com",
    "smtp.password" -> "123456qwerty",
    "ehcacheplugin" -> "disabled"
  ) ++ SensitiveConfigurationData.config

  val additionalPlugins = Seq(
    "mock.FixedEhCachePlugin",
    "com.typesafe.plugin.CommonsMailerPlugin",
    "com.github.kompot.play2sec.authentication.providers.oauth1.twitter.TwitterAuthProvider",
    "com.github.kompot.play2sec.authentication.providers.oauth2.facebook.FacebookAuthProvider",
    "com.github.kompot.play2sec.authentication.providers.oauth2.google.GoogleAuthProvider",
    "com.github.kompot.play2sec.authentication.DefaultPlaySecPlugin"
  )

  val normalUser = Seq(
    "com.github.kompot.play2sec.authentication.providers.MyUsernamePasswordAuthProvider"
  )

  val shortLivingUser = Seq(
    "com.github.kompot.play2sec.authentication.providers.MyUsernamePasswordAuthProviderShortLiving"
  )

  val noAutoLink = Map(
    "play2sec.accountAutoLink" -> "false"
  )

  val noAutoMerge = Map(
    "play2sec.accountAutoMerge" -> "false"
  )
}
