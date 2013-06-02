/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth2

import com.github.kompot.play2sec.authentication.providers.AuthInfo
import com.github.kompot.play2sec.authentication.user.AuthUser

abstract case class OAuth2AuthInfo(accessToken: String, expires: Long) extends AuthInfo {
  def this(accessToken: String) = this(accessToken, AuthUser.NO_EXPIRATION)
}
