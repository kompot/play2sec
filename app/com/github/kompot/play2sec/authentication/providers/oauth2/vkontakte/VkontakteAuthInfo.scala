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

import java.util.Date
import play.api.libs.json.JsValue
import com.github.kompot.play2sec.authentication.providers.oauth2.OAuth2AuthInfo
import com.github.kompot.play2sec.authentication.providers.oauth2.OAuth2AuthProvider.Constants

class VkontakteAuthInfo(node: JsValue) extends OAuth2AuthInfo(
  node.\(Constants.ACCESS_TOKEN).as[Option[String]].getOrElse(""),
  new Date().getTime + node.\(Constants.EXPIRES_IN).as[Option[Long]].getOrElse(0L) * 1000
) {
  val bearer = node.\(Constants.TOKEN_TYPE).as[Option[String]].getOrElse("")
  val idToken = node.\("id_token").as[Option[String]].getOrElse("")
}
