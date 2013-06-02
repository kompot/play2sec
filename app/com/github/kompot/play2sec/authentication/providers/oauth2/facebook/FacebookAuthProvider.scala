/*
 * Copyright (c) 2013.
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
import com.github.kompot.play2sec.authentication.exceptions.AccessTokenException

class FacebookAuthProvider(app: play.api.Application) extends OAuth2AuthProvider[FacebookAuthUser, FacebookAuthInfo](app) {
  override def getKey = FacebookAuthProvider.PROVIDER_KEY

  protected override def transform(info: Future[FacebookAuthInfo], state: String): FacebookAuthUser = {
    val futureUser = for {
      fai <- info
      r <- WS
          .url(getConfiguration.getString(FacebookAuthProvider.USER_INFO_URL_SETTING_KEY).get)
          .withQueryString((OAuth2AuthProvider.Constants.ACCESS_TOKEN, fai.accessToken)).get()
      if r.json.\(OAuth2AuthProvider.Constants.ERROR).toString() == "null"
    } yield {
      new FacebookAuthUser(r.json, fai, state)
    }
    // TODO: get rid of Await
    Await.result(futureUser, 10 seconds)
  }

  protected override def buildInfo(fr: Future[Response]): Future[FacebookAuthInfo] = {
    fr.map { r: Response =>
//      Logger.warn("status is " + r.status + " -- " + r.json)
      if (r.status >= 400)
        throw new AccessTokenException(r.json.\(FacebookAuthProvider.MESSAGE).toString())
      val query: String = r.body
      Logger.debug(query)
      val pairs: List[NameValuePair] = URLEncodedUtils.parse(URI.create("/?" + query), "utf-8").toList
      if (pairs.size < 2) {
        throw new AccessTokenException()
      }
      new FacebookAuthInfo(pairs.map { kv => (kv.getName, kv.getValue) }.toMap[String, String])
    }
  }
}

object FacebookAuthProvider {
  val PROVIDER_KEY = "facebook"
  private val MESSAGE = "message"
  private val USER_INFO_URL_SETTING_KEY = "userInfoUrl"
}
