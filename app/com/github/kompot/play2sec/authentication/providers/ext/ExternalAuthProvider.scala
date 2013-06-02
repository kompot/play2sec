/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.ext

import play.api.mvc.{Call, Request}
import scala.Predef.String
import play.api.Play.current
import com.github.kompot.play2sec.authentication.PlaySecPlugin
import com.github.kompot.play2sec.authentication.providers.AuthProvider

abstract class ExternalAuthProvider(app: play.api.Application) extends AuthProvider(app) {
  override def isExternal = true

  override protected def neededSettingKeys: List[String] = List.empty

  private object SettingKeys {
    val REDIRECT_URI_HOST: String = "redirectUri.host"
    val REDIRECT_URI_SECURE: String = "redirectUri.secure"
  }

  private def useSecureRedirectUri: Boolean = getConfiguration.getBoolean(SettingKeys.REDIRECT_URI_SECURE).getOrElse(false)

  protected def getRedirectUrl[A](request: Request[A]): String = {
    val overrideHost: String = getConfiguration.getString(SettingKeys.REDIRECT_URI_HOST).getOrElse(null)
    val isHttps: Boolean = useSecureRedirectUri
    val c: Call = com.typesafe.plugin.use[PlaySecPlugin].auth(getKey)
    if (overrideHost != null && !overrideHost.trim.isEmpty) {
      "http" + (if (isHttps) "s" else "") + "://" + overrideHost + c.url
    } else {
      c.absoluteURL(isHttps)(request)
    }
  }
}
