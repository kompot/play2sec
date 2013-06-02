/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.exceptions

class AccessTokenException(message: String) extends AuthException(message) {
  def this() = this("")
}
