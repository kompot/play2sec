/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth2.facebook

import java.util.Date
import com.github.kompot.play2sec.authentication.providers.oauth2
.{OAuth2AuthProvider, OAuth2AuthInfo}

class FacebookAuthInfo(m: Map[String, String]) extends OAuth2AuthInfo(
  m.get(OAuth2AuthProvider.Constants.ACCESS_TOKEN).get,
  new Date().getTime + m.get("expires").head.toLong * 1000) {
}
