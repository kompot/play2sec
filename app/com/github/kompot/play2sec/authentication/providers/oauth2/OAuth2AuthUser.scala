/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth2

import com.github.kompot.play2sec.authentication.user.AuthUser

abstract class OAuth2AuthUser(id: String, info: OAuth2AuthInfo, state: String) extends AuthUser {
  override def getId = id
  override def expires = info.expires
}
