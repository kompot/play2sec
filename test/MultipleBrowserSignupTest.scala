import bootstrap.Global.Injector
import com.github.kompot.play2sec.authentication.providers.oauth1.twitter
.TwitterAuthProvider
import com.github.kompot.play2sec.authentication.providers.oauth2.facebook
.FacebookAuthProvider
import com.github.kompot.play2sec.authentication.providers.oauth2.google
.GoogleAuthProvider
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

class MultipleBrowserSignupTest extends PlaySpecification {
  sequential

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

  "Accounts email, facebook, twitter and google should be merged" in new WithBrowser(FIREFOX, new FakeApp) {

    val facebookLogin    = current.configuration.getString("test.facebook.login")
    val facebookPassword = current.configuration.getString("test.facebook.password")
    val facebookUserId   = current.configuration.getString("test.facebook.id")

    val twitterLogin    = current.configuration.getString("test.twitter.login")
    val twitterPassword = current.configuration.getString("test.twitter.password")
    val twitterUserId   = current.configuration.getString("test.twitter.id")

    val googleLogin    = current.configuration.getString("test.google.login")
    val googlePassword = current.configuration.getString("test.google.password")
    val googleUserId   = current.configuration.getString("test.google.id")

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

    assert(googleLogin.isDefined && !googleLogin.get.isEmpty,
      "Key test.google.login is not defined in configuration.")
    assert(googlePassword.isDefined && !googlePassword.get.isEmpty,
      "Key test.google.password is not defined in configuration.")
    assert(googleUserId.isDefined && !googleUserId.get.isEmpty,
      "Key test.google.id is not defined in configuration.")

    // first signup as a normal email/password user
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
    val user = Await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
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

    val user1 = Await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = TwitterAuthProvider.PROVIDER_KEY
      def id = twitterUserId.get
    }))
    user1.get.remoteUsers.size mustEqual 3

    browser.goTo("/auth/external/google")
    browser.waitUntil(10, TimeUnit.SECONDS) {
      browser.url.contains("google.com/ServiceLogin")
    }

    browser.fill("input#Email").`with`(googleLogin.get)
    browser.fill("input#Passwd").`with`(googlePassword.get)
    browser.click("input[type = 'submit'][id = 'signIn']")

    browser.waitUntil(10, TimeUnit.SECONDS) {
      browser.url.contains("accounts.google.com/o/oauth2/auth")
    }

    browser.waitUntil(10, TimeUnit.SECONDS) {
      browser.findFirst("button[id = 'submit_approve_access']").isEnabled
    }

    browser.click("button[id = 'submit_approve_access']")

    browser.waitUntil(10, TimeUnit.SECONDS) {
      browser.url.startsWith("/")
    }

    val user2 = Await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = GoogleAuthProvider.PROVIDER_KEY
      def id = googleUserId.get
    }))
    user2.get.remoteUsers.size mustEqual 4
  }

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }
}
