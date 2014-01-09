import bootstrap.Global.Injector
import com.github.kompot.play2sec.authentication.providers.oauth1.twitter
.TwitterAuthProvider
import com.github.kompot.play2sec.authentication.providers.oauth2.facebook
.FacebookAuthProvider
import com.github.kompot.play2sec.authentication.providers.oauth2.google
.GoogleAuthProvider
import com.github.kompot.play2sec.authentication.providers.password
.UsernamePasswordAuthProvider
import com.github.kompot.play2sec.authentication.user.AuthUserIdentity
import java.util.concurrent.TimeUnit
import mock.MailServer
import model.Await
import play.api.Play
import play.api.test.{WithBrowser, PlaySpecification}
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.api.Play.current
import util.StringUtils

class LinkTest extends PlaySpecification {
  sequential

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

  "Should be asked for linking accounts and answer yes" in new WithBrowser(FIREFOX, new FakeAppNoAutoLink) {
    val facebookLogin    = current.configuration.getString("test.facebook.login")
    val facebookPassword = current.configuration.getString("test.facebook.password")
    val facebookUserId   = current.configuration.getString("test.facebook.id")

    assert(facebookLogin.isDefined && !facebookLogin.get.isEmpty,
      "Key test.facebook.login is not defined in configuration.")
    assert(facebookPassword.isDefined && !facebookPassword.get.isEmpty,
      "Key test.facebook.password is not defined in configuration.")
    assert(facebookUserId.isDefined && !facebookUserId.get.isEmpty,
      "Key test.facebook.id is not defined in configuration.")

    Injector.userStore.getStore.size mustEqual 0

    browser.goTo("/auth/signup")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text("123")
    browser.click("input[type = 'submit']")

    Injector.userStore.getStore.size mustEqual 1

    val link = StringUtils.getFirstLinkByContent(
      MailServer.boxes("kompotik@gmail.com").findByContent("verify-email")(0).body, "verify-email").get
    val emailVerificationLink = StringUtils.getRequestPathFromString(link)

    browser.goTo(emailVerificationLink)

    Injector.userStore.getStore.size mustEqual 1

    // then signup via facebook
    browser.goTo("/auth/external/facebook")
    browser.waitUntil(10, TimeUnit.SECONDS) {
      browser.url.contains("www.facebook.com/login")
    }
    if (browser.url.contains("www.facebook.com/login")) {
      browser.fill("input#email").`with`(facebookLogin.get)
      browser.fill("input#pass").`with`(facebookPassword.get)
      browser.click("input[type = 'submit'][name = 'login']")
    }
    browser.waitUntil(10, TimeUnit.SECONDS) {
      // wait until back to localhost
      browser.url.startsWith("/auth/ask-link")
    }
    val userBefore = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = FacebookAuthProvider.PROVIDER_KEY
      def id = facebookUserId.get
    }))
    // there should be no such user as we asked no to auto link
    // it should present only in some kind of a transient storage
    userBefore mustEqual None
    browser.click("input#link")
    browser.click("input[type = 'submit']")

    Injector.userStore.getStore.size mustEqual 1

    val user = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = FacebookAuthProvider.PROVIDER_KEY
      def id = facebookUserId.get
    }))
    user.get mustNotEqual None
    // we checked checkbox - should be single account
    user.get.remoteUsers.size mustEqual 2

    Injector.userStore.getStore.size mustEqual 1
  }

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

  "Should be asked for linking accounts and answer no" in new WithBrowser(FIREFOX, new FakeAppNoAutoLink) {
    val facebookLogin    = current.configuration.getString("test.facebook.login")
    val facebookPassword = current.configuration.getString("test.facebook.password")
    val facebookUserId   = current.configuration.getString("test.facebook.id")

    assert(facebookLogin.isDefined && !facebookLogin.get.isEmpty,
      "Key test.facebook.login is not defined in configuration.")
    assert(facebookPassword.isDefined && !facebookPassword.get.isEmpty,
      "Key test.facebook.password is not defined in configuration.")
    assert(facebookUserId.isDefined && !facebookUserId.get.isEmpty,
      "Key test.facebook.id is not defined in configuration.")

    Injector.userStore.getStore.size mustEqual 0

    browser.goTo("/auth/signup")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text("123")
    browser.click("input[type = 'submit']")

    Injector.userStore.getStore.size mustEqual 1

    val link = StringUtils.getFirstLinkByContent(
      MailServer.boxes("kompotik@gmail.com").findByContent("verify-email")(0).body, "verify-email").get
    val emailVerificationLink = StringUtils.getRequestPathFromString(link)

    browser.goTo(emailVerificationLink)

    Injector.userStore.getStore.size mustEqual 1

    // then signup via facebook
    browser.goTo("/auth/external/facebook")
    browser.waitUntil(10, TimeUnit.SECONDS) {
      browser.url.contains("www.facebook.com/login")
    }
    if (browser.url.contains("www.facebook.com/login")) {
      browser.fill("input#email").`with`(facebookLogin.get)
      browser.fill("input#pass").`with`(facebookPassword.get)
      browser.click("input[type = 'submit'][name = 'login']")
    }
    browser.waitUntil(10, TimeUnit.SECONDS) {
      // wait until back to localhost
      browser.url.startsWith("/auth/ask-link")
    }
    val userBefore = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = FacebookAuthProvider.PROVIDER_KEY
      def id = facebookUserId.get
    }))
    // there should be no such user as we asked no to auto link
    // it should present only in some kind of a transient storage
    userBefore mustEqual None
    browser.click("input[type = 'submit']")

    Injector.userStore.getStore.size mustEqual 2

    val user = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = FacebookAuthProvider.PROVIDER_KEY
      def id = facebookUserId.get
    }))
    user.get mustNotEqual None
    // we checked checkbox - should be single account
    user.get.remoteUsers.size mustEqual 1

    Injector.userStore.getStore.size mustEqual 2
  }

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }
}
