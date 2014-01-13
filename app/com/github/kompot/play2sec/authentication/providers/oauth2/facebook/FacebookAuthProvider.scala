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

package com.github.kompot.play2sec.authentication.providers.oauth2.facebook

import play.api.libs.ws.{WS, Response}
import play.api.Logger
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils
import java.net.URI
import concurrent.{Await, ExecutionContext, Future}
import concurrent.duration._
import scala.collection.JavaConversions._
import ExecutionContext.Implicits.global
import scala.language.postfixOps
import com.github.kompot.play2sec.authentication.providers.oauth2
.OAuth2AuthProvider
import com.github.kompot.play2sec.authentication.exceptions.{AuthException,
AccessTokenException}
import play.api.libs.json.JsUndefined

class FacebookAuthProvider(app: play.api.Application) extends OAuth2AuthProvider[FacebookAuthUser, FacebookAuthInfo](app) {
  override val key = FacebookAuthProvider.PROVIDER_KEY

  protected override def transform(info: Future[FacebookAuthInfo], state: String): Future[FacebookAuthUser] =
    for {
      fai <- info
      r <- WS
          .url(providerConfig.getString("userInfoUrl").get)
          .withQueryString((OAuth2AuthProvider.Constants.ACCESS_TOKEN, fai.accessToken)).get()
    } yield {
      if (r.json.\(OAuth2AuthProvider.Constants.ERROR).isInstanceOf[JsUndefined]) {
        new FacebookAuthUser(r.json, fai, state)
      } else {
        throw new AuthException(r.json.\(OAuth2AuthProvider.Constants.ERROR).toString())
      }
    }

  protected override def buildInfo(fr: Future[Response]): Future[FacebookAuthInfo] =
    for {
      r <- fr
    } yield {
      Logger.info(key + " response status is " + r.status + " and content is " + r.body)
      if (r.status != 200) {
        throw new AccessTokenException(s"Unable to create $key auth info from response " + r)
      }
      val body: String = r.body
      Logger.info(body)
      val pairs = URLEncodedUtils.parse(URI.create("/?" + body), "utf-8").toList
      if (pairs.size < 2) {
        throw new AccessTokenException()
      }
      new FacebookAuthInfo(pairs.map(kv => (kv.getName, kv.getValue)).toMap[String, String])
    }

}

object FacebookAuthProvider {
  val PROVIDER_KEY = "facebook"
}
