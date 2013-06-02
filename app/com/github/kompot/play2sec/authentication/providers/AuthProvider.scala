/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers

import play.api.{Configuration, Plugin}
import play.api.mvc.{Request, AnyContent}
import play.api.Play.current
import com.typesafe.plugin._
import com.github.kompot.play2sec.authentication.PlaySecPlugin
import com.github.kompot.play2sec.authentication.exceptions.AuthException
import com.github.kompot.play2sec.authentication.providers.password.Case
import com.github.kompot.play2sec.authentication.user.{SessionAuthUser,
AuthUser}

abstract case class AuthProvider(app: play.api.Application) extends Plugin {
  override def onStart() {
    def neededSettings = neededSettingKeys
    if (neededSettings.size > 0) {
      val c = getConfiguration
      if (c == null) {
        throw new RuntimeException(s"No settings for provider '$getKey' available at all!")
      }
      for (key <- neededSettings ) {
        val setting = c.getString(key)
        if (setting == null || "".equals(setting)) {
          throw new RuntimeException("Provider '" + getKey
              + "' missing needed setting '" + key + "'")
        }
      }
    }
    register(getKey, this)
  }


  override def onStop() {
    unregister(getKey)
  }

  def getUrl: String = use[PlaySecPlugin].auth(getKey).url

  def getAbsoluteUrl(request: Request[AnyContent]): String = {
    use[PlaySecPlugin]
        .auth(getKey)
        // TODO: determine whether secure or not in runtime
        .absoluteURL(secure = false)(request)
  }

  def getKey: String

  def getConfiguration: Configuration = {
    com.github.kompot.play2sec.authentication.getConfiguration.get.getConfig(getKey).get
  }

  /**
   * Returns either an AuthUser object or a String (URL)
   *
   * @param request
   * @param payload
	 * Some arbitrary payload that shall get passed into the
   * authentication process
   * @return
   * @throws AuthException
   */
  @throws(classOf[AuthException])
  def authenticate[A](request: Request[A], payload: Option[Case.Value]): Any

  protected def neededSettingKeys: List[String]

  def getSessionAuthUser(id: String, expires: Long): AuthUser = {
    new SessionAuthUser(id, getKey, expires)
  }

  def isExternal: Boolean
}
