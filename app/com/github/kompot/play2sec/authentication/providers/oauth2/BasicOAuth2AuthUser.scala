/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth2

import com.github.kompot.play2sec.authentication.user.{AuthUser, BasicIdentity}

abstract class BasicOAuth2AuthUser(id: String, info: OAuth2AuthInfo, state: String)
    extends OAuth2AuthUser(id, info, state) with BasicIdentity {
  override def toString = AuthUser.toString
}
