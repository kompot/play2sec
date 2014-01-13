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

package com.github.kompot.play2sec.authentication.providers.oauth2

import play.api.{Logger, Configuration}
import play.api.mvc.Request
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import scala.collection.JavaConversions._
import play.api.libs.ws.{WS, Response}
import concurrent.Future
import org.apache.http.client.methods.HttpGet
import com.github.kompot.play2sec.authentication.user.AuthUserIdentity
import com.github.kompot.play2sec.authentication.providers.ext
.ExternalAuthProvider
import com.github.kompot.play2sec.authentication.providers.password
.{LoginSignupResult, Case}
import com.github.kompot.play2sec.authentication.exceptions
.{RedirectUriMismatch, AuthException, AccessDeniedException}
import scala.concurrent.ExecutionContext.Implicits.global

abstract class OAuth2AuthProvider[U <: BasicOAuth2AuthUser, I <: OAuth2AuthInfo](app: play.api.Application)
    extends ExternalAuthProvider(app) {

  override protected def requiredSettings = super.requiredSettings ++ List(
    OAuth2AuthProvider.SettingKeys.ACCESS_TOKEN_URL,
    OAuth2AuthProvider.SettingKeys.AUTHORIZATION_URL,
    OAuth2AuthProvider.SettingKeys.CLIENT_ID,
    OAuth2AuthProvider.SettingKeys.CLIENT_SECRET
  )

  def getAccessTokenParams[A](c: Configuration, code: String, request: Request[A]): String = {
    import OAuth2AuthProvider._

    val params: List[NameValuePair] = getParams(request, c) ++ List(
      new BasicNameValuePair(Constants.CLIENT_SECRET, c.getString(SettingKeys.CLIENT_SECRET).get),
      new BasicNameValuePair(Constants.GRANT_TYPE, Constants.AUTHORIZATION_CODE),
      new BasicNameValuePair(Constants.CODE, code)
    )

    URLEncodedUtils.format(params, "UTF-8")
  }

  def getAccessToken[A](code: String, request: Request[A]): Future[I] = {
    val c = providerConfig
    val params = getAccessTokenParams(c, code, request)
    val url: String = c.getString(OAuth2AuthProvider.SettingKeys.ACCESS_TOKEN_URL).get
    val headers = ("Content-Type", "application/x-www-form-urlencoded")
    buildInfo(WS.url(url).withHeaders(headers).post(params))
  }

  protected def buildInfo(r: Future[Response]): Future[I]

  protected def getAuthUrl[A](request: Request[A], state: String): String = {
    import OAuth2AuthProvider._

    val c: Configuration = providerConfig

    val params: List[NameValuePair] = getParams(request, c) ++ List(
      new BasicNameValuePair(Constants.SCOPE, c.getString(SettingKeys.SCOPE).get),
      new BasicNameValuePair(Constants.RESPONSE_TYPE, Constants.CODE),
//      TODO: null? is there any possibility of null
//      if (state != null) {
        new BasicNameValuePair(OAuth2AuthProvider.Constants.STATE, state)
//      }
    )

    val m: HttpGet = new HttpGet(
      c.getString(SettingKeys.AUTHORIZATION_URL).get + "?"
          + URLEncodedUtils.format(params, "UTF-8"))

    m.getURI.toString
  }

  def getParams[A](request: Request[A], c: Configuration): List[NameValuePair] = {
    import OAuth2AuthProvider._
//    val params: List[NameValuePair] = new ArrayList[NameValuePair]();
//    params.add(new BasicNameValuePair(Constants.CLIENT_ID, c
//        .getString(SettingKeys.CLIENT_ID)));
//    params.add(new BasicNameValuePair(Constants.REDIRECT_URI,
//      getRedirectUrl(request)));
//    return params;
    List[NameValuePair](
      new BasicNameValuePair(Constants.CLIENT_ID, c.getString(SettingKeys.CLIENT_ID).get),
      new BasicNameValuePair(Constants.REDIRECT_URI, getRedirectUrl(request))
    )
  }

  override def authenticate[A](request: Request[A], payload: Option[Case]) = {
    import OAuth2AuthProvider._

    Logger.info(s"Returned with URL ${request.uri}")

    val error: Option[String] = request.getQueryString(Constants.ERROR)
    val code: Option[String] = request.getQueryString(Constants.CODE)

    // Attention: facebook does *not* support state that is non-ASCII - not
    // even encoded.
    val state = request.getQueryString(Constants.STATE).getOrElse("")

    (error, code, state) match {
      case (Some(e), _, _) => {
        if (e == Constants.ACCESS_DENIED) {
          throw new AccessDeniedException(key)
        } else if (e == Constants.REDIRECT_URI_MISMATCH) {
          Logger.error("You must set the redirect URI for your provider to " +
              "whatever you defined in your routes file. For " +
              "this provider it is: '" + getRedirectUrl(request) + "'")
          throw new RedirectUriMismatch
        } else {
          throw new AuthException(e)
        }
      }
      case (_, Some(c), _) =>
        for {
          tr <- transform(getAccessToken(c, request), state)
        } yield {
          new LoginSignupResult(tr)
        }
      case _ => {
        // no auth, yet
        val url = getAuthUrl(request, state)
        Logger.info("generated redirect URL for dialog: " + url)
        Future.successful(new LoginSignupResult(url))
      }
    }
  }

  /**
   * This allows custom implementations to enrich an AuthUser object or
   * provide their own implementation
   *
   * @param info
   * @param state
   * @return
   * @throws AuthException
   */
  @throws(classOf[AuthException])
  protected def transform(info: Future[I], state: String): Future[U]
}

object OAuth2AuthProvider {
  object SettingKeys {
    val AUTHORIZATION_URL = "authorizationUrl"
    val ACCESS_TOKEN_URL = "accessTokenUrl"
    val CLIENT_ID = "clientId"
    val CLIENT_SECRET = "clientSecret"
    val SCOPE = "scope"
  }

  object Constants {
    val CLIENT_ID = "client_id"
    val CLIENT_SECRET = "client_secret"
    val REDIRECT_URI = "redirect_uri"
    val SCOPE = "scope"
    val RESPONSE_TYPE = "response_type"
    val STATE = "state"
    val GRANT_TYPE = "grant_type"
    val AUTHORIZATION_CODE = "authorization_code"
    val ACCESS_TOKEN = "access_token"
    val ERROR = "error"
    val CODE = "code"
    val TOKEN_TYPE = "token_type"
    val EXPIRES_IN = "expires_in"
    val REFRESH_TOKEN = "refresh_token"
    val ACCESS_DENIED = "access_denied"
    val REDIRECT_URI_MISMATCH = "redirect_uri_mismatch"
  }
}
