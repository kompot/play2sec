/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.password

case class SessionUsernamePasswordAuthUser(clearPassword: String, email: String, exp: Long)
    extends UsernamePasswordAuthUser(clearPassword, email) {

  override def expires = exp
}
