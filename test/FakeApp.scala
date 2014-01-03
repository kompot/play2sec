import bootstrap.Global.Injector
import com.github.kompot.play2sec.authentication
import com.github.kompot.play2sec.authentication.providers.password
.UsernamePasswordAuthProvider
import com.github.kompot.play2sec.authentication.user.AuthUser
import controllers.{JsonWebConversions, JsResponseError, Authorization}
import model.MongoWait
import play.api.libs.json.JsValue
import play.api.mvc.{Handler, Results, Action}
import play.api.templates.Html
import play.api.test.FakeApplication
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FakeApp extends FakeApplication(
  withRoutes = FakeApp.routes,
  additionalConfiguration = FakeApp.additionalConfiguration,
  additionalPlugins = FakeApp.additionalPlugins)

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
          JsResponseError("Unable to perform signup.", Some(errors)))) },
        { case _ => UsernamePasswordAuthProvider.handleSignup(request) }
        )
      }
    case ("GET", "/auth") =>
      Action { implicit request =>
        Results.Ok(
          Html("""
            <form action="/auth/signup" method="post">
              <input type="text"     name="email"    id="email" />
              <input type="password" name="password" id="password" />
              <input type="submit" />
            </form>
               """))
      }
    case ("GET", "/auth/verify-email") =>
      Action.async { implicit request =>
        for {
          maybeToken <- Injector.tokenService.getValidTokenBySecurityKey(request.getQueryString("token").get)
          email = maybeToken.get.data.\("email").as[String]
          res <- Injector.userService.verifyEmail(maybeToken.get.userId, email)
          maybeUser <- Injector.userService.get(maybeToken.get.userId)
        } yield {
          if (maybeUser.isDefined) {
            if (maybeUser.get.remoteUsers.exists{ r =>
              r.provider == UsernamePasswordAuthProvider.PROVIDER_KEY &&
                  r.id == email && r.isConfirmed
            }) {
              val identity = new AuthUser {
                def id = email
                def provider = UsernamePasswordAuthProvider.PROVIDER_KEY
              }
              MongoWait(authentication.loginAndRedirect(request, Future.successful(identity)))
            } else {
              Results.InternalServerError("Email was not verified.")
            }
          } else {
            Results.InternalServerError("Email was not verified.")
          }
        }
      }
    case ("GET", "/") => Action(Results.Ok("It's home baby"))
  }
  val additionalConfiguration = Map(
    "mongodb.db" -> "play2sec-test",
    "mongodb.servers" -> List("localhost:12345"),
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
    "play.modules.reactivemongo.ReactiveMongoPlugin",
    "com.typesafe.plugin.CommonsMailerPlugin",
    "com.github.kompot.play2sec.authentication.providers.MyUsernamePasswordAuthProvider",
    "com.github.kompot.play2sec.authentication.providers.oauth1.twitter.TwitterAuthProvider",
    "com.github.kompot.play2sec.authentication.providers.oauth2.facebook.FacebookAuthProvider",
    "com.github.kompot.play2sec.authentication.providers.oauth2.google.GoogleAuthProvider",
    "com.github.kompot.play2sec.authentication.DefaultPlaySecPlugin"
  )
}
