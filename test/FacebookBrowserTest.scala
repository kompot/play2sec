import bootstrap.Global.Injector
import com.github.kompot.play2sec.authentication
import com.github.kompot.play2sec.authentication.providers.oauth1.twitter
.TwitterAuthProvider
import com.github.kompot.play2sec.authentication.providers.oauth2.facebook
.FacebookAuthProvider
import com.github.kompot.play2sec.authentication.providers.password
.UsernamePasswordAuthProvider
import com.github.kompot.play2sec.authentication.user.{AuthUser,
AuthUserIdentity}
import com.google.common.base.Optional
import controllers.{JsonWebConversions, Authorization, JsResponseError}
import java.util.concurrent.TimeUnit
import mock.MailServer
import model.{MongoWait, UserService}
import org.joda.time.Seconds
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.JsValue
import play.api.Logger
import play.api.mvc.{Results, Action}
import play.api.templates.Html
import play.api.test.FakeApplication
import play.api.test.{FakeApplication, WithBrowser, PlaySpecification}
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import play.api.Play.current
import util.StringUtils

object Helper extends JsonWebConversions {
  val appWithRoutes = FakeApplication(withRoutes = {
    case ("GET", "/auth/external/facebook") =>
      Action.async { implicit request =>
        authentication.handleAuthentication("facebook", request)
      }
    case ("GET", "/auth/external/twitter") =>
      Action.async { implicit request =>
        authentication.handleAuthentication("twitter", request)
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
  }, additionalConfiguration = Map(
    "mongodb.db" -> "play2sec-test",
    "application.secret" -> "123",
    "application.global" -> "bootstrap.Global",
    // this is your test account facebook real login
    "test.facebook.login" -> "",
    // password
    "test.facebook.password" -> "",
    // id of your facebook user account
    "test.facebook.id" -> "",
    // clientId of your test application
    "play2sec.facebook.clientId" -> "",
    // clientSecret of it
    "play2sec.facebook.clientSecret" -> "",
    "play2sec.facebook.authorizationUrl" -> "https://graph.facebook.com/oauth/authorize",
    "play2sec.facebook.accessTokenUrl" -> "https://graph.facebook.com/oauth/access_token",
    "play2sec.facebook.userInfoUrl" -> "https://graph.facebook.com/me",
    "play2sec.facebook.scope" -> "email",
    "test.twitter.login" -> "",
    "test.twitter.password" -> "",
    "test.twitter.id" -> "",
    "play2sec.twitter.consumerKey" -> "",
    "play2sec.twitter.consumerSecret" -> "",
    "play2sec.twitter.requestTokenUrl" -> "https://api.twitter.com/oauth/request_token",
    "play2sec.twitter.accessTokenUrl" -> "https://api.twitter.com/oauth/access_token",
    "play2sec.twitter.authorizationUrl" -> "https://api.twitter.com/oauth/authorize",
    "play2sec.twitter.userInfoUrl" -> "https://api.twitter.com/1.1/account/verify_credentials.json",
    "play2sec.twitter.redirectUri.secure" -> "false",
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
    "smtp.password" -> "123456qwerty"
  ), additionalPlugins = Seq(
    "play.modules.reactivemongo.ReactiveMongoPlugin",
    "com.typesafe.plugin.CommonsMailerPlugin",
    "com.github.kompot.play2sec.authentication.providers.MyUsernamePasswordAuthProvider",
    "com.github.kompot.play2sec.authentication.providers.oauth1.twitter.TwitterAuthProvider",
    "com.github.kompot.play2sec.authentication.providers.oauth2.facebook.FacebookAuthProvider",
    "com.github.kompot.play2sec.authentication.DefaultPlaySecPlugin"
  ))
}

class FacebookBrowserTest extends PlaySpecification {
  "Accounts email and facebook should be merged" in new WithBrowser(
    webDriver = FIREFOX, app = Helper.appWithRoutes) {

    val facebookLogin    = current.configuration.getString("test.facebook.login")
    val facebookPassword = current.configuration.getString("test.facebook.password")
    val facebookUserId   = current.configuration.getString("test.facebook.id")

    val twitterLogin    = current.configuration.getString("test.twitter.login")
    val twitterPassword = current.configuration.getString("test.twitter.password")
    val twitterUserId   = current.configuration.getString("test.twitter.id")

    assert(facebookLogin.isDefined && !facebookLogin.get.isEmpty,
      "Key test.facebook.login is not defined in configuration.")
    assert(facebookPassword.isDefined && !facebookPassword.get.isEmpty,
      "Key test.facebook.password is not defined in configuration.")
    assert(facebookUserId.isDefined && !facebookUserId.get.isEmpty,
      "Key test.facebook.id is not defined in configuration.")

    assert(twitterLogin.isDefined && !twitterLogin.get.isEmpty,
      "Key test.twitter.login is not defined in configuration.")
    assert(twitterPassword.isDefined && !twitterPassword.get.isEmpty,
      "Key test.twitter.password is not defined in configuration.")
    assert(twitterUserId.isDefined && !twitterUserId.get.isEmpty,
      "Key test.twitter.id is not defined in configuration.")

//    // first signup as a normal email/password user
    browser.goTo("/auth")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text("123")
    browser.click("input#noaccount")
    browser.click("input[type = 'submit']")

    browser.await().atMost(1000)

    val link = StringUtils.getFirstLinkByContent(
      MailServer.boxes("kompotik@gmail.com").findByContent("verify-email")(0).body, "verify-email").get
    val emailVerificationLink = StringUtils.getRequestPathFromString(link)

    browser.goTo(emailVerificationLink)

    // then signup via facebook
    browser.goTo("/auth/external/facebook")
    browser.waitUntil(10, TimeUnit.SECONDS) {
      browser.url.contains("www.facebook.com/login")
    }
    if (browser.url.contains("www.facebook.com/login")) {
      browser.fill("input#email").`with`(facebookLogin.get)
      browser.fill("input#pass").`with`(facebookPassword.get)
      browser.click("input#persist_box")
      browser.click("input[type = 'submit'][name = 'login']")
    }

    browser.waitUntil(10, TimeUnit.SECONDS) {
      // wait until back to localhost
      browser.url.startsWith("/")
    }
    val user = MongoWait(new UserService().getByAuthUserIdentity(new AuthUserIdentity {
      def provider = FacebookAuthProvider.PROVIDER_KEY
      def id = facebookUserId.get
    }))
    user.get mustNotEqual None
    user.get.remoteUsers.size mustEqual 2
    // TODO when doing signup in this order
    // 1. email 2. facebook
    // then facebook user is confirmed and it's ok
    // but when
    // 1. facebook
    // it's not and it's definitely a bug
    user.get.confirmed mustEqual true

    browser.goTo("/auth/external/twitter")
    browser.waitUntil(10, TimeUnit.SECONDS) {
      browser.url.contains("api.twitter.com/oauth/authorize")
    }

    browser.fill("input#username_or_email").`with`(twitterLogin.get)
    browser.fill("input#password").`with`(twitterPassword.get)
    browser.click("input[type = 'submit'][id = 'allow']")

    browser.waitUntil(10, TimeUnit.SECONDS) {
      // wait until back to localhost
      browser.url.startsWith("/")
    }

    val user1 = MongoWait(new UserService().getByAuthUserIdentity(new AuthUserIdentity {
      def provider = TwitterAuthProvider.PROVIDER_KEY
      def id = twitterUserId.get
    }))
    user1.get.remoteUsers.size mustEqual 3

  }
}
