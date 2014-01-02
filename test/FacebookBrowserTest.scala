import com.github.kompot.play2sec.authentication
import com.github.kompot.play2sec.authentication.providers.oauth2.facebook
.FacebookAuthProvider
import com.github.kompot.play2sec.authentication.user.AuthUserIdentity
import java.util.concurrent.TimeUnit
import model.{MongoWait, UserService}
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.JsValue
import play.api.Logger
import play.api.mvc.{Results, Action}
import play.api.test.FakeApplication
import play.api.test.{FakeApplication, WithBrowser, PlaySpecification}
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.api.Play.current

object Helper {
  val appWithRoutes = FakeApplication(withRoutes = {
    case ("GET", "/auth/external/facebook") =>
      Action.async { implicit request =>
        authentication.handleAuthentication("facebook", request)
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
    "play2sec.facebook.scope" -> "email"
  ), additionalPlugins = Seq(
    "play.modules.reactivemongo.ReactiveMongoPlugin",
    "com.github.kompot.play2sec.authentication.providers.oauth2.facebook.FacebookAuthProvider",
    "com.github.kompot.play2sec.authentication.DefaultPlaySecPlugin"
  ))
}

class FacebookBrowserTest extends PlaySpecification {
  "Accounts email and facebook should be merged" in new WithBrowser(
    webDriver = FIREFOX, app = Helper.appWithRoutes) {

    val login          = current.configuration.getString("test.facebook.login")
    val password       = current.configuration.getString("test.facebook.password")
    val facebookUserId = current.configuration.getString("test.facebook.id")

    assert(login.isDefined && !login.get.isEmpty,
      "Key test.facebook.login is not defined in configuration.")
    assert(password.isDefined && !password.get.isEmpty,
      "Key test.facebook.password is not defined in configuration.")
    assert(facebookUserId.isDefined && !facebookUserId.get.isEmpty,
      "Key test.facebook.id is not defined in configuration.")

//    // first signup as a normal email/password user
//    browser.goTo(routes.Authorization.logIn().url)
//    browser.fill("input#email").`with`("kompotik@gmail.com")
//    browser.$("#password").text("123")
//    browser.click("input#noaccount")
//    browser.click("input[type = 'button'][id = 'submit']")
//
//    browser.await().atMost(1000)

    // then signup via facebook
    browser.goTo("/auth/external/facebook")
    browser.waitUntil(10, TimeUnit.SECONDS) {
      browser.url.contains("www.facebook.com/login")
    }
    if (browser.url.contains("www.facebook.com/login")) {
      browser.fill("input#email").`with`(login.get)
      browser.fill("input#pass").`with`(password.get)
      browser.click("input#persist_box")
      browser.click("input[type = 'submit'][name = 'login']")
    }

    browser.waitUntil(10, TimeUnit.SECONDS) {
      // wait until back to localhost
      println("___" + browser.url)
      browser.url.startsWith("/")
    }
    val user = MongoWait(new UserService().getByAuthUserIdentity(new AuthUserIdentity {
      def provider = FacebookAuthProvider.PROVIDER_KEY
      def id = facebookUserId.get
    }))
    user.get mustNotEqual None
    user.get.remoteUsers.size mustEqual 1
    // TODO should definitely be true
//    user.get.confirmed mustEqual true

    //    new UserService().getByAuthUserIdentity(new AuthUserIdentity {
//      def getId = "123"
//      def getProvider = FacebookAuthProvider.PROVIDER_KEY
//    }).map{ u =>
//    // verify that our user has two remote accounts
//      u mustNotEqual None
//      u.get.remoteUsers.size must beEqualTo(2)
//    }
  }
}
