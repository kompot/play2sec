/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth1

import com.github.kompot.play2sec.authentication.user.{AuthUser,
AuthUserIdentity, NameIdentity}

abstract class BasicOAuth1AuthUser(id: String, info: OAuth1AuthInfo, state: String)
    extends OAuth1AuthUser(id, info, state)
    with NameIdentity with AuthUserIdentity {
  override def toString = AuthUser.toString
}
