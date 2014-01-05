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

package com.github.kompot.play2sec.authentication.providers.ext

import play.api.mvc.Request
import play.api.Play.current
import com.github.kompot.play2sec.authentication.PlaySecPlugin
import com.github.kompot.play2sec.authentication.providers.AuthProvider

abstract class ExternalAuthProvider(app: play.api.Application) extends AuthProvider(app) {
  override val isExternal = true

  override protected def requiredSettings: List[String] = List.empty

  private object SettingKeys {
    val REDIRECT_URI_HOST = "redirectUri.host"
    val REDIRECT_URI_SECURE = "redirectUri.secure"
  }

  protected def getRedirectUrl[A](request: Request[A]): String = {
    val overrideHost = providerConfig.getString(SettingKeys.REDIRECT_URI_HOST)
    val isHttps = providerConfig.getBoolean(SettingKeys.REDIRECT_URI_SECURE).getOrElse(false)
    val c = com.typesafe.plugin.use[PlaySecPlugin].auth(key)
    overrideHost match {
      case Some(oh) => "http" + (if (isHttps) "s" else "") + "://" + oh + c.url
      case _        => c.absoluteURL(isHttps)(request)
    }
  }
}
