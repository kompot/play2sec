/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth2.google

import java.util.Date

import com.github.kompot.play2sec.authentication.providers.oauth2.OAuth2AuthInfo
import com.github.kompot.play2sec.authentication.providers.oauth2.OAuth2AuthProvider.Constants
import com.github.kompot.play2sec.authentication.providers.oauth2.OAuth2AuthInfo
import play.api.libs.json.JsValue
import com.github.kompot.play2sec.authentication.providers.oauth2.OAuth2AuthInfo

class GoogleAuthInfo(node: JsValue) extends OAuth2AuthInfo(
  node.\(Constants.ACCESS_TOKEN).as[Option[String]].getOrElse(""),
  new Date().getTime + node.\(Constants.EXPIRES_IN).as[Option[Long]].getOrElse(0L) * 1000
) {
  val bearer = node.\(Constants.TOKEN_TYPE).as[Option[String]].getOrElse("")
  val idToken = node.\("id_token").as[Option[String]].getOrElse("")
}
