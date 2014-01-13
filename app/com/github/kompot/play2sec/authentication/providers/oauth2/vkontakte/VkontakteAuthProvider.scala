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

package com.github.kompot.play2sec.authentication.providers.oauth2.vkontakte

import play.api.{Logger, Application}
import play.api.libs.ws.{Response, WS}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import com.github.kompot.play2sec.authentication.providers.oauth2
.OAuth2AuthProvider
import com.github.kompot.play2sec.authentication.exceptions
.{AccessTokenException, AuthException}
import play.api.libs.json.JsUndefined

class VkontakteAuthProvider(app: Application)
    extends OAuth2AuthProvider[VkontakteAuthUser, VkontakteAuthInfo](app) {

  val USER_INFO_URL_SETTING_KEY = "userInfoUrl"

  override val key = VkontakteAuthProvider.PROVIDER_KEY

  protected override def transform(info: Future[VkontakteAuthInfo], state: String): Future[VkontakteAuthUser] =
    for {
      i <- info
      r <- WS.url(providerConfig.getString(USER_INFO_URL_SETTING_KEY).get)
          .withQueryString((OAuth2AuthProvider.Constants.ACCESS_TOKEN, i.accessToken))
          .get()
    } yield {
      if (r.json.\(OAuth2AuthProvider.Constants.ERROR).isInstanceOf[JsUndefined]) {
        new VkontakteAuthUser(r.json, i, state)
      } else {
        throw new AuthException(r.json.\(OAuth2AuthProvider.Constants.ERROR).toString())
      }
    }

  @throws(classOf[AccessTokenException])
  protected override def buildInfo(fr: Future[Response]): Future[VkontakteAuthInfo] =
    // TODO generalize first 7 lines of this method (same as in Facebook and Goole providers)
    for {
      r <- fr
    } yield {
      Logger.info(key + " response status is " + r.status + " and content is " + r.body)
      if (r.status != 200) {
        throw new AccessTokenException(s"Unable to create $key auth info from response " + r)
      }
      if (r.json.\(OAuth2AuthProvider.Constants.ERROR).isInstanceOf[JsUndefined]) {
        new VkontakteAuthInfo(r.json)
      } else {
        throw new AccessTokenException(r.json.\(OAuth2AuthProvider.Constants.ERROR).toString())
      }
    }
}

object VkontakteAuthProvider {
  val PROVIDER_KEY = "vkontakte"
}
