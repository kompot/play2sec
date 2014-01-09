package com.github.kompot.play2sec

import bootstrap.Global.Injector
import com.github.kompot.play2sec.authentication.providers.oauth2.facebook
.FacebookAuthProvider
import com.github.kompot.play2sec.authentication.user.AuthUserIdentity
import java.util.concurrent.TimeUnit
import mock.MailServer
import play.api.Play._
import play.api.test.{PlaySpecification, WithBrowser}
import util.StringUtils
import play.api.Logger
import com.github.kompot.play2sec.authentication.providers.password
.UsernamePasswordAuthProvider
import play.api.mvc.Session
import com.github.kompot.play2sec.authentication

class MergeTest extends PlaySpecification {
  sequential

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

  "Should be asked for merging accounts and answer yes" in new WithBrowser(FIREFOX, new FakeAppNoAutoMerge) {
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
    browser.goTo("/auth/logout")

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
    val user = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = FacebookAuthProvider.PROVIDER_KEY
      def id = facebookUserId.get
    }))
    // there should be such separate user as we asked noе to auto merge
    user mustNotEqual None

    Injector.userStore.getStore.size mustEqual 2

    // here we should have 2 separate users in storage
    // and be logged in as a facebook user

    // when trying to log in with email/password we should be asked to merge
    // these 2 accounts

    browser.goTo("/auth/login")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text("123")
    browser.click("input[type = 'submit']")

    browser.waitUntil(1, TimeUnit.SECONDS) {
      browser.url.startsWith("/auth/ask-merge")
    }
    browser.click("input#merge")
    browser.click("input[type = 'submit']")


    val userAfterMerge = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = FacebookAuthProvider.PROVIDER_KEY
      def id = facebookUserId.get
    }))
    // we checked checkbox - should be two merged accounts account
    userAfterMerge.get.remoteUsers.size mustEqual 2
    Injector.userStore.getStore.size mustEqual 1
  }

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

  "Should be asked for merging accounts and answer no" in new WithBrowser(FIREFOX, new FakeAppNoAutoMerge) {
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
    browser.goTo("/auth/logout")

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
    val user = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = FacebookAuthProvider.PROVIDER_KEY
      def id = facebookUserId.get
    }))
    user mustNotEqual None

    Injector.userStore.getStore.size mustEqual 2

    // here we should have 2 separate users in storage
    // and be logged in as a facebook user

    // when trying to log in with email/password we should be asked to merge
    // these 2 accounts

    browser.goTo("/auth/login")
    browser.fill("input#email").`with`("kompotik@gmail.com")
    browser.$("#password").text("123")
    browser.click("input[type = 'submit']")

    browser.waitUntil(1, TimeUnit.SECONDS) {
      browser.url.startsWith("/auth/ask-merge")
    }
    browser.click("input[type = 'submit']")

    // and still have 2 separate accounts but be logged in under email account

    val userAsBeforeMerge = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
      def provider = UsernamePasswordAuthProvider.PROVIDER_KEY
      def id = "kompotik@gmail.com"
    }))
    // we checked checkbox - should be two merged accounts account
    userAsBeforeMerge.get.remoteUsers.size mustEqual 1
    Injector.userStore.getStore.size mustEqual 2

    val value = browser.getCookie("PLAY_SESSION").getValue
    val playSession1 = Session(Session.decode(value.substring(1, value.length - 1)))
    authentication.isLoggedIn(playSession1) mustEqual true
    authentication.getUser(playSession1) mustNotEqual None
    authentication.getUser(playSession1).get.provider mustEqual UsernamePasswordAuthProvider.PROVIDER_KEY
  }

  step {
    Injector.tokenStore.clearStore()
    Injector.userStore.clearStore()
  }

}
