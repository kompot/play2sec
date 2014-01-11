package com.github.kompot.play2sec

import play.api.test.{WithBrowser, PlaySpecification}
import bootstrap.Global.Injector
import java.util.concurrent.TimeUnit
import util.StringUtils
import mock.MailServer
import com.github.kompot.play2sec.authentication.user.AuthUserIdentity
import com.github.kompot.play2sec.authentication.providers.password
.UsernamePasswordAuthProvider

class BasicAuthorizationTest extends PlaySpecification {
  sequential

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

  "access to a restricted page should be denied" in new WithBrowser(FIREFOX, new FakeApp) {
    browser.goTo("/auth/authenticated-only")
    browser.pageSource().contains("Access denied") mustEqual true
    browser.goTo("/auth/not-authenticated-only")
    browser.pageSource().contains("visible only to not authenticated users") mustEqual true
  }

  private val password = "123"

  "access to a restricted page should be allowed when authorized" in new WithBrowser(FIREFOX, new FakeApp) {
    browser.goTo("/auth/signup")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text(password)
    browser.click("input[type = 'submit']")

    val link = StringUtils.getFirstLinkByContent(
      MailServer.boxes("kompotik@gmail.com").findByContent("verify-email")(0).body, "verify-email").get
    val emailVerificationLink = StringUtils.getRequestPathFromString(link)

    browser.goTo(emailVerificationLink)

    browser.goTo("/auth/login")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text(password)
    browser.click("input[type = 'submit']")

    browser.goTo("/auth/authenticated-only")
    browser.pageSource().contains("visible only to authenticated users") mustEqual true
    browser.goTo("/auth/not-authenticated-only")
    browser.pageSource().contains("Access denied") mustEqual true
  }

  "access to a page with admin only access should be allowed" in new WithBrowser(FIREFOX, new FakeApp) {
    browser.goTo("/auth/admin-only")
    browser.pageSource().contains("Access denied") mustEqual true

    val user = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = UsernamePasswordAuthProvider.PROVIDER_KEY
      def id = "kompotik@gmail.com"
    }))

    user.get mustNotEqual None
    Injector.userStore.put(user.get._id, user.get.copy(roles = Set("admin")))

    browser.goTo("/auth/logout")
    browser.goTo("/auth/login")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text(password)
    browser.click("input[type = 'submit']")

    browser.goTo("/auth/admin-only")
    browser.pageSource().contains("visible only to users with admin role") mustEqual true
  }

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

}
