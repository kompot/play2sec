import bootstrap.Global.Injector
import com.github.kompot.play2sec
import com.github.kompot.play2sec.authentication
import com.github.kompot.play2sec.authentication.providers.oauth2.facebook
.FacebookAuthProvider
import com.github.kompot.play2sec.authentication.providers.password
.UsernamePasswordAuthProvider
import com.github.kompot.play2sec.authentication.user.AuthUserIdentity
import java.util.concurrent.TimeUnit
import mock.MailServer
import play.api.mvc.{Cookie, Session}
import play.api.test.{WithBrowser, PlaySpecification}
import scala.concurrent.duration._
import util.StringUtils

class LoginPasswordTest extends PlaySpecification {
  sequential

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

  "should not be able to log in with unconfirmed account" in new WithBrowser(FIREFOX, new FakeApp) {
    private val password = "123"

    browser.goTo("/auth/signup")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text(password)
    browser.click("input[type = 'submit']")

    browser.await().atMost(1000)

    val user = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = UsernamePasswordAuthProvider.PROVIDER_KEY
      def id = "kompotik@gmail.com"
    }))

    user.get mustNotEqual None
    user.get.remoteUsers.size mustEqual 1

    // should not allow registration with the same email

    browser.goTo("/auth/signup")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text(password)
    browser.click("input[type = 'submit']")

    browser.await().atMost(1000)

    browser.pageSource().contains("This email has already been used") mustEqual true

    // try to log in with bad password - should fail

    browser.goTo("/auth/login")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text("wrong-password")
    browser.click("input[type = 'submit']")

    browser.pageSource().contains("Wrong login or password") mustEqual true

    // try to log in with unconfirmed user account - should fail

    browser.goTo("/auth/login")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text(password)
    browser.click("input[type = 'submit']")

    browser.await().atMost(1000)
    browser.url.contains("/auth/user-unverified") mustEqual true

    val link = StringUtils.getFirstLinkByContent(
      MailServer.boxes("kompotik@gmail.com").findByContent("verify-email")(0).body, "verify-email").get
    val emailVerificationLink = StringUtils.getRequestPathFromString(link)

    browser.goTo(emailVerificationLink)

    browser.goTo("/auth/login")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text(password)
    browser.click("input[type = 'submit']")

    browser.await().atMost(1000)
    browser.url.contains("/after-auth") mustEqual true

    browser.getCookie("PLAY_SESSION") mustNotEqual null
    browser.goTo("/auth/logout")
    // cookie should get cleared after logout
    browser.getCookie("PLAY_SESSION") mustEqual null
    browser.url.contains("/after-logout") mustEqual true
  }

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

  "user should be logged in with standard session length" in new WithBrowser(FIREFOX, new FakeApp) {
    private val password = "123"

    browser.goTo("/auth/signup")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text(password)
    browser.click("input[type = 'submit']")

    val link = StringUtils.getFirstLinkByContent(
      MailServer.boxes("kompotik@gmail.com").findByContent("verify-email")(0).body, "verify-email").get
    val emailVerificationLink = StringUtils.getRequestPathFromString(link)

    browser.goTo(emailVerificationLink)

    browser.await().atMost(1000)
    browser.url.contains("/after-auth") mustEqual true

    val value = browser.getCookie("PLAY_SESSION").getValue
    val playSession1 = Session(Session.decode(value.substring(1, value.length - 1)))
    authentication.isLoggedIn(playSession1) mustEqual true
  }

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

  "user should NOT be logged in with short lived session" in new WithBrowser(FIREFOX, new FakeAppShortLivingUser) {
    private val password = "123"

    browser.goTo("/auth/signup")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text(password)
    browser.click("input[type = 'submit']")

    val link = StringUtils.getFirstLinkByContent(
      MailServer.boxes("kompotik@gmail.com").findByContent("verify-email")(0).body, "verify-email").get
    val emailVerificationLink = StringUtils.getRequestPathFromString(link)

    browser.goTo(emailVerificationLink)

    browser.await().atMost(1000)
    browser.url.contains("/after-auth") mustEqual true

    val value = browser.getCookie("PLAY_SESSION").getValue
    val playSession1 = Session(Session.decode(value.substring(1, value.length - 1)))
    authentication.isLoggedIn(playSession1) mustEqual false
  }

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }
}
