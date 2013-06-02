/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth1.twitter

import com.github.kompot.play2sec.authentication.providers.oauth1.OAuth1AuthInfo

case class TwitterAuthInfo(token: String, tokenSecret: String)
    extends OAuth1AuthInfo(token, tokenSecret)
