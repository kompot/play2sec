package com.github.kompot.play2sec

import bootstrap.Global.Injector
import com.github.kompot.play2sec.authentication.user.AuthUserIdentity
import java.util.concurrent.TimeUnit
import play.api.Play._
import play.api.test.{WithBrowser, PlaySpecification}
import com.github.kompot.play2sec.authentication.providers.oauth2.vkontakte
.VkontakteAuthProvider

class VkontakteSignupTest extends PlaySpecification {
   sequential

   step {
     Injector.tokenStore.clearStore()
     Injector.userStore.clearStore()
   }

   "new user signed up with Vkontakte should be confirmed" in new WithBrowser(FIREFOX, new FakeApp) {
     val vkontakteLogin    = current.configuration.getString("test.vkontakte.login")
     val vkontaktePassword = current.configuration.getString("test.vkontakte.password")
     val vkontakteUserId   = current.configuration.getString("test.vkontakte.id")

     browser.goTo("/auth/external/vkontakte")
     browser.waitUntil(10, TimeUnit.SECONDS) {
       browser.url.contains("oauth.vk.com/authorize")
     }
     if (browser.url.contains("oauth.vk.com/authorize")) {
       browser.fill("input[name = 'email']").`with`(vkontakteLogin.get)
       browser.fill("input[name = 'pass']").`with`(vkontaktePassword.get)
       browser.click("button[type = 'submit']")
     }

     browser.waitUntil(10, TimeUnit.SECONDS) {
       // wait until back to localhost
       browser.url.startsWith("/")
     }

     val user = await(Injector.userStore.getByAuthUserIdentity(new AuthUserIdentity {
       def provider = VkontakteAuthProvider.PROVIDER_KEY
       def id = vkontakteUserId.get
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
