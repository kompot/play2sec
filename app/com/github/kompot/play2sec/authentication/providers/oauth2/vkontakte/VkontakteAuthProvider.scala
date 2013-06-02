/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth2.vkontakte

import play.api.Application
import play.api.libs.ws.{Response, WS}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import com.github.kompot.play2sec.authentication.providers.oauth2
.OAuth2AuthProvider
import com.github.kompot.play2sec.authentication.exceptions
.{AccessTokenException, AuthException}

class VkontakteAuthProvider(app: Application)
    extends OAuth2AuthProvider[VkontakteAuthUser, VkontakteAuthInfo](app) {

	val USER_INFO_URL_SETTING_KEY = "userInfoUrl"

  override def getKey = VkontakteAuthProvider.PROVIDER_KEY

  protected override def transform(info: Future[VkontakteAuthInfo], state: String): VkontakteAuthUser = {
    val futureUser = for {
      i <- info
      r <- WS.url(getConfiguration.getString(USER_INFO_URL_SETTING_KEY).get)
          .withQueryString((OAuth2AuthProvider.Constants.ACCESS_TOKEN, i.accessToken))
          .get()
    } yield {
      val err = r.json.\(OAuth2AuthProvider.Constants.ERROR).as[Option[String]].getOrElse(null)
      if (err != null) {
        throw new AuthException(err)
      } else {
        new VkontakteAuthUser(r.json, i, state)
      }
    }
    Await.result(futureUser, 10 seconds)
	}

  @throws(classOf[AccessTokenException])
  protected override def buildInfo(fr: Future[Response]): Future[VkontakteAuthInfo] = {
    for {
      r <- fr
    } yield {
      val err = r.json.\(OAuth2AuthProvider.Constants.ERROR).as[Option[String]].getOrElse(null)
      if (err != null) {
        throw new AccessTokenException(err)
      } else {
        new VkontakteAuthInfo(r.json)
      }
    }
	}
}

object VkontakteAuthProvider {
  val PROVIDER_KEY = "vkontakte"
}
