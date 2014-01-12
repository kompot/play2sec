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

package com.github.kompot.play2sec.authentication.providers.oauth1

import play.api.Application
import play.api.Logger

import play.api.mvc.{Session, Request}
import scala._
import play.api.libs.oauth.OAuth
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.ConsumerKey
import com.github.kompot.play2sec.authentication.user.{AuthUser,
AuthUserIdentity}
import com.github.kompot.play2sec.authentication.providers.ext.ExternalAuthProvider
import com.github.kompot.play2sec.authentication.exceptions.{AuthException, AccessTokenException}
import com.github.kompot.play2sec.authentication.providers.password
.{LoginSignupResult, Case}
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

abstract class OAuth1AuthProvider[U <: BasicOAuth1AuthUser, I <: OAuth1AuthInfo](app: Application)
    extends ExternalAuthProvider(app) {

  @throws(classOf[AccessTokenException])
  def buildInfo(toke: RequestToken): I

  override protected def requiredSettings = super.requiredSettings ++ List(
    OAuth1AuthProvider.SettingKeys.ACCESS_TOKEN_URL,
    OAuth1AuthProvider.SettingKeys.AUTHORIZATION_URL,
    OAuth1AuthProvider.SettingKeys.REQUEST_TOKEN_URL,
    OAuth1AuthProvider.SettingKeys.CONSUMER_KEY,
    OAuth1AuthProvider.SettingKeys.CONSUMER_SECRET
  )

  override def authenticate[A](request: Request[A], payload: Option[Case]) = {
    import OAuth1AuthProvider._

    val uri = request.uri

    Logger.info("Returned with URL: '" + uri + "'")

    val c = providerConfig

    val key = new ConsumerKey(
      c.getString(SettingKeys.CONSUMER_KEY).get,
      c.getString(SettingKeys.CONSUMER_SECRET).get)
    val requestTokenURL = c.getString(SettingKeys.REQUEST_TOKEN_URL).get
    val accessTokenURL = c.getString(SettingKeys.ACCESS_TOKEN_URL).get
    val authorizationURL = c.getString(SettingKeys.AUTHORIZATION_URL).get
    val info = new ServiceInfo(requestTokenURL, accessTokenURL, authorizationURL, key)
    val service = new OAuth(info, true)

    if (uri.contains(Constants.OAUTH_VERIFIER)) {
      val rtoken = com.github.kompot.play2sec.authentication.removeFromCache(request.session, CACHE_TOKEN).asInstanceOf[Option[RequestToken]]
      val verifier = request.getQueryString(Constants.OAUTH_VERIFIER)

      val b = service.retrieveAccessToken(rtoken.get, verifier.getOrElse("")).fold(e => {
        throw new AuthException(e.getLocalizedMessage)
      }, token => {
        val i: I = buildInfo(token)
        transform(i)
      })
      Future.successful(new LoginSignupResult(b))
    } else {
      val callbackURL = getRedirectUrl(request)
      val res = service.retrieveRequestToken(callbackURL).fold(e => {
        throw new AuthException(e.getLocalizedMessage)
      }, requestToken => {
        val token = requestToken.token
        val redirectUrl = service.redirectUrl(token)
        val session = com.github.kompot.play2sec.authentication.storeInCache(request.session, CACHE_TOKEN, requestToken)
        (redirectUrl, session)
      })
      Future.successful(new LoginSignupResult(res._1, res._2))
    }

  }

  /**
   * This allows custom implementations to enrich an AuthUser object or
   * provide their own implementation
   */
  @throws(classOf[AuthException])
  def transform(identity: I): U
}

object OAuth1AuthProvider {
  val CACHE_TOKEN = "p2s.oauth1.rtoken"

  object SettingKeys {
    val REQUEST_TOKEN_URL = "requestTokenUrl"
    val AUTHORIZATION_URL = "authorizationUrl"
    val ACCESS_TOKEN_URL = "accessTokenUrl"
    val CONSUMER_KEY = "consumerKey"
    val CONSUMER_SECRET = "consumerSecret"
  }

  object Constants {
    val OAUTH_TOKEN_SECRET = "oauth_token_secret"
    val OAUTH_TOKEN = "oauth_token"
    val OAUTH_VERIFIER = "oauth_verifier"
  }

}
