/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.exceptions

case class AuthException(message: String) extends Exception {
  def this() = this("")
}
