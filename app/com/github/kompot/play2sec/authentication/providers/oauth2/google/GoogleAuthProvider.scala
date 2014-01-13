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

package com.github.kompot.play2sec.authentication.providers.oauth2.google

import play.api.{Logger, Application}

import play.api.libs.ws.{Response, WS}
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.language.postfixOps
import com.github.kompot.play2sec.authentication.providers.oauth2
.OAuth2AuthProvider
import com.github.kompot.play2sec.authentication.exceptions
.{AccessTokenException, AuthException}
import play.api.libs.json.JsUndefined

class GoogleAuthProvider(app: Application)
    extends OAuth2AuthProvider[GoogleAuthUser, GoogleAuthInfo](app) {

  override val key = GoogleAuthProvider.PROVIDER_KEY

  protected override def transform(info: Future[GoogleAuthInfo], state: String): Future[GoogleAuthUser] =
    for {
      i <- info
      r <- WS.url(providerConfig.getString("userInfoUrl").get)
          .withQueryString((OAuth2AuthProvider.Constants.ACCESS_TOKEN, i.accessToken))
          .get()
    } yield {
      if (r.json.\(OAuth2AuthProvider.Constants.ERROR).isInstanceOf[JsUndefined]) {
        new GoogleAuthUser(r.json, i, state)
      } else {
        throw new AuthException(r.json.\(OAuth2AuthProvider.Constants.ERROR).toString())
      }
    }

  @throws(classOf[AccessTokenException])
  protected override def buildInfo(fr: Future[Response]): Future[GoogleAuthInfo] =
    for {
      r <- fr
    } yield {
      Logger.info(key + " response status is " + r.status + " and content is " + r.body)
      // TODO: google sometimes returns error (in html)
      // and r.json fails with
      // JsonParseException: Unexpected character ('<' (code 60)): expected a valid value (number, String, array, object, 'true', 'false' or 'null')
      // at [Source: [B@136c16e; line: 1, column: 2]]
      // how to fix that?
      if (r.status != 200) {
        throw new AccessTokenException(s"Unable to create $key auth info from response " + r)
      }
      if (r.json.\(OAuth2AuthProvider.Constants.ERROR).isInstanceOf[JsUndefined]) {
        new GoogleAuthInfo(r.json)
      } else {
        throw new AccessTokenException(r.json.\(OAuth2AuthProvider.Constants.ERROR).toString())
      }
    }

}

object GoogleAuthProvider {
  val PROVIDER_KEY = "google"
}
