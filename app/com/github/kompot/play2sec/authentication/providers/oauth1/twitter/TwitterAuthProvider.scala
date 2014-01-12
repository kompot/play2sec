/*
 * Copyright 2012-2014 Joscha Feth, Steve Chaloner, Anton Fedchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.kompot.play2sec.authentication.providers.oauth1.twitter

import play.api.Application
import play.api.libs.oauth.ConsumerKey
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.oauth.RequestToken
import play.api.libs.ws.WS
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.language.postfixOps
import com.github.kompot.play2sec.authentication.providers.oauth1
.OAuth1AuthProvider
import com.github.kompot.play2sec.authentication.exceptions
.{AccessTokenException, AuthException}

class TwitterAuthProvider(app: Application) extends
OAuth1AuthProvider[TwitterAuthUser, TwitterAuthInfo](app) {
  val USER_INFO_URL_SETTING_KEY = "userInfoUrl"

  override val key = TwitterAuthProvider.PROVIDER_KEY

  @throws(classOf[AuthException])
  override def transform(info: TwitterAuthInfo): TwitterAuthUser = {
    val url = providerConfig.getString(USER_INFO_URL_SETTING_KEY).get

    val token = new RequestToken(info.token, info.tokenSecret)
    val c = providerConfig
    val cK = new ConsumerKey(
      c.getString(OAuth1AuthProvider.SettingKeys.CONSUMER_KEY).get,
      c.getString(OAuth1AuthProvider.SettingKeys.CONSUMER_SECRET).get)
    val futureUser = for {
      r <- WS.url(url).sign(new OAuthCalculator(cK, token)).get()
    } yield {
      new TwitterAuthUser(r.json, info)
    }
    // TODO: get rid of Await, make it work with futures
    Await.result(futureUser, 10 seconds)
  }

  @throws(classOf[AccessTokenException])
  override def buildInfo(rtoken: RequestToken): TwitterAuthInfo = {
    new TwitterAuthInfo(rtoken.token, rtoken.secret)
  }

}

object TwitterAuthProvider {
  val PROVIDER_KEY = "twitter"
}
