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

package com.github.kompot.play2sec.authentication.providers.oauth2.vkontakte

import play.api.libs.json.JsValue
import com.github.kompot.play2sec.authentication.providers.oauth2
.BasicOAuth2AuthUser
import com.github.kompot.play2sec.authentication.user.AuthUserIdentity

class VkontakteAuthUser(node: JsValue, info: VkontakteAuthInfo, state: String)
    extends BasicOAuth2AuthUser(node.\("response")(0).\(VkontakteAuthUser.Constants.ID).as[Option[Int]].getOrElse(0).toString, info, state)
    with AuthUserIdentity {

  val firstName = node.\("response")(0).\(VkontakteAuthUser.Constants.FIRST_NAME).as[Option[String]].getOrElse("")
  val lastName =  node.\("response")(0).\(VkontakteAuthUser.Constants.LAST_NAME) .as[Option[String]].getOrElse("")

  override def provider = VkontakteAuthProvider.PROVIDER_KEY

  override def id = node.\("response")(0).\(VkontakteAuthUser.Constants.ID).as[Option[Int]].getOrElse(0).toString
  def email = ""
  def name = lastName + " " + firstName
  def getFirstName = firstName
  def getLastName = lastName
}

object VkontakteAuthUser {
  object Constants {
    val ID = "uid" // "00000000000000",
    val FIRST_NAME = "first_name" // "Fred",
    val LAST_NAME = "last_name" // "Example",
  }
}
