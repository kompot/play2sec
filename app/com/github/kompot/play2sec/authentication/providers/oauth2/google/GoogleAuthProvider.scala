/*
 * Copyright 2012-2013 Joscha Feth, Steve Chaloner, Anton Fedchenko
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

class GoogleAuthProvider(app: Application)
    extends OAuth2AuthProvider[GoogleAuthUser, GoogleAuthInfo](app) {

  val USER_INFO_URL_SETTING_KEY = "userInfoUrl"

  override val key = GoogleAuthProvider.PROVIDER_KEY

  protected override def transform(info: Future[GoogleAuthInfo], state: String): GoogleAuthUser = {
    val futureUser = for {
      i <- info
      r <- WS.url(providerConfig.getString(USER_INFO_URL_SETTING_KEY).get)
          .withQueryString((OAuth2AuthProvider.Constants.ACCESS_TOKEN, i.accessToken))
          .get()
    } yield {
      val err = r.json.\(OAuth2AuthProvider.Constants.ERROR).as[Option[String]].getOrElse(null)
      if (err != null) {
        throw new AuthException(err)
      } else {
        new GoogleAuthUser(r.json, i, state)
      }
    }
    Await.result(futureUser, 10 seconds)
  }

  @throws(classOf[AccessTokenException])
  protected override def buildInfo(fr: Future[Response]): Future[GoogleAuthInfo] = {
    for {
      r <- fr
    } yield {
      Logger.info("google response status is " + r.status + " and content is " + r.body)
      // TODO: google sometimes returns error (in html)
      // and r.json fails with
      // JsonParseException: Unexpected character ('<' (code 60)): expected a valid value (number, String, array, object, 'true', 'false' or 'null')
      // at [Source: [B@136c16e; line: 1, column: 2]]
      // how to fix that?
      if (r.status != 200) {
        throw new AccessTokenException("Unable to create GoogleAuthInfo from response " + r)
      }
      val err = r.json.\(OAuth2AuthProvider.Constants.ERROR).as[Option[String]].getOrElse(null)
      if (err != null) {
        throw new AccessTokenException(err)
      } else {
        new GoogleAuthInfo(r.json)
      }
    }
  }
}

object GoogleAuthProvider {
  val PROVIDER_KEY = "google"
}
