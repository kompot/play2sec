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

import java.util.Date
import com.github.kompot.play2sec.authentication.providers.oauth2
.{OAuth2AuthProvider, OAuth2AuthInfo}

class FacebookAuthInfo(m: Map[String, String]) extends OAuth2AuthInfo(
  m.get(OAuth2AuthProvider.Constants.ACCESS_TOKEN).get,
  new Date().getTime + m.get("expires").head.toLong * OAuth2AuthInfo.msInS) {
}
