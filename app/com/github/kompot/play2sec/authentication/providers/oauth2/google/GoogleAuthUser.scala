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

import play.api.libs.json.JsValue
import com.github.kompot.play2sec.authentication.providers.oauth2.BasicOAuth2AuthUser
import com.github.kompot.play2sec.authentication.user._

/**
 * From https://developers.google.com/accounts/docs/OAuth2Login#userinfocall
 */
class GoogleAuthUser(node: JsValue, info: GoogleAuthInfo, state: String)
    extends BasicOAuth2AuthUser((node \ "id").as[Option[String]].getOrElse(""), info, state)
    with ExtendedIdentity with PicturedIdentity with ProfiledIdentity with LocaleIdentity {

  val email =           (node \ "email")         .as[Option[String]].getOrElse("")
  val emailIsVerified = (node \ "verified_email").as[Option[Boolean]].getOrElse(false)
  val name =            (node \ "name")          .as[Option[String]].getOrElse("")
  val firstName =       (node \ "given_name")    .as[Option[String]].getOrElse("")
  val lastName =        (node \ "family_name")   .as[Option[String]].getOrElse("")
  val picture =         (node \ "picture")       .as[Option[String]].getOrElse("")
  val gender =          (node \ "gender")        .as[Option[String]].getOrElse("")
  val loc =             (node \ "locale")        .as[Option[String]].getOrElse("")
  val link =            (node \ "link")          .as[Option[String]].getOrElse("")

  override def provider = GoogleAuthProvider.PROVIDER_KEY

  override def id = (node \ "id").as[Option[String]].getOrElse("")
  def isEmailVerified = emailIsVerified
  override def profileLink = link
  override def locale = AuthUser.getLocaleFromString(loc).get
}
