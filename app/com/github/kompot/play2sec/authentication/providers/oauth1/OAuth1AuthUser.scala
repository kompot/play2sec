/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth1

import com.github.kompot.play2sec.authentication.user.AuthUser

abstract class OAuth1AuthUser(id: String, info: OAuth1AuthInfo, state: String)
    extends AuthUser
