import bootstrap.Global.Injector
import com.github.kompot.play2sec.authentication.providers.oauth2.facebook
.FacebookAuthProvider
import com.github.kompot.play2sec.authentication.user.AuthUserIdentity
import java.util.concurrent.TimeUnit
import mock.UserStore
import model.Await
import play.api.Play
import play.api.Play._
import play.api.test.{FakeApplication, Helpers, WithBrowser, PlaySpecification}
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

class FacebookSignupTest extends PlaySpecification {
  sequential

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

  "new user signed up with Facebook should be confirmed" in new WithBrowser(FIREFOX, new FakeApp) {
    val facebookLogin    = current.configuration.getString("test.facebook.login")
    val facebookPassword = current.configuration.getString("test.facebook.password")
    val facebookUserId   = current.configuration.getString("test.facebook.id")

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

    val user = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = FacebookAuthProvider.PROVIDER_KEY
      def id = facebookUserId.get
    }))

    user.get mustNotEqual None
    user.get.remoteUsers.size mustEqual 1
    user.get.confirmed mustEqual true
  }

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }
}
