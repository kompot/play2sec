/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth2.google

import play.api.Application

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

  override def getKey = GoogleAuthProvider.PROVIDER_KEY

  protected override def transform(info: Future[GoogleAuthInfo], state: String): GoogleAuthUser = {
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
