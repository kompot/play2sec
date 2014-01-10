package com.github.kompot.play2sec

import play.api.test.{WithBrowser, PlaySpecification}
import bootstrap.Global.Injector
import java.util.concurrent.TimeUnit
import util.StringUtils
import mock.MailServer

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

  "access to a restricted page should be allowed when authorized" in new WithBrowser(FIREFOX, new FakeApp) {
    private val password = "123"

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

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

}
