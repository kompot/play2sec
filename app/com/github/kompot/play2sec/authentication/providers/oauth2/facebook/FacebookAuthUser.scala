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

package com.github.kompot.play2sec.authentication.providers.oauth2.facebook

import play.api.libs.json.JsValue
import com.github.kompot.play2sec.authentication.providers.oauth2.BasicOAuth2AuthUser
import com.github.kompot.play2sec.authentication.user._

class FacebookAuthUser(node: JsValue, info: FacebookAuthInfo, state: String)
  extends BasicOAuth2AuthUser(node.\(FacebookAuthUser.Constants.ID).as[String], info, state) with ExtendedIdentity
  with PicturedIdentity with ProfiledIdentity with LocaleIdentity {

  import FacebookAuthUser.Constants._

  val id = node.\(FacebookAuthUser.Constants.ID).as[String]
  val name = node.\(NAME).as[Option[String]].getOrElse("")
  val firstName = node.\(FIRST_NAME).as[Option[String]].getOrElse("")
  val lastName = node.\(LAST_NAME).as[Option[String]].getOrElse("")
  val link = node.\(LINK).as[Option[String]].getOrElse("")
  val username = node.\(USERNAME).as[Option[String]].getOrElse("")
  val gender = node.\(GENDER).as[Option[String]].getOrElse("")
  val email = node.\(EMAIL).as[Option[String]].getOrElse("")
  val verified = node.\(VERIFIED).as[Option[Boolean]].getOrElse(false)
  val timeZone = node.\(TIME_ZONE).as[Option[Int]].getOrElse(0)
  val loc = node.\(LOCALE).as[Option[String]].getOrElse("")
  val updateTime = node.\(UPDATE_TIME).as[Option[String]].getOrElse("")

  override def provider = FacebookAuthProvider.PROVIDER_KEY

  override def profileLink = link
  def getUsername = username
  def isVerified = verified
  def getTimeZone = timeZone
  // According to https://developers.facebook.com/docs/reference/api/#pictures
  override def picture = s"https://graph.facebook.com/$username/picture"
  override def locale = AuthUser.getLocaleFromString(loc).get
  def getUpdateTime = updateTime
}

object FacebookAuthUser {
  object Constants {
    val ID = "id" // "616473731"
    val NAME = "name" // "Joscha Feth"
    val FIRST_NAME = "first_name"// "Joscha"
    val LAST_NAME = "last_name" // "Feth"
    val LINK = "link" // "http://www.facebook.com/joscha.feth"
    val USERNAME = "username"// "joscha.feth"
    val GENDER = "gender"// "male"
    val EMAIL = "email"// "joscha@feth.com"
    val TIME_ZONE = "timezone"// 2
    val LOCALE = "locale"// "de_DE"
    val VERIFIED = "verified"// true
    val UPDATE_TIME = "updated_time" // "2012-04-26T20:22:52+0000"}
  }
}
