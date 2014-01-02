/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authentication.providers

import com.github.kompot.play2sec.authentication.providers.password.DefaultUsernamePasswordAuthUser

class MyLoginUsernamePasswordAuthUser(clearPassword: String, email: String,
    expiration: Long) extends DefaultUsernamePasswordAuthUser(clearPassword, email) {

  def this(clearPassword: String, email: String) =
    this(clearPassword, email, System.currentTimeMillis + 14 * 24 * 3600 * 1000)

  override def expires = expiration
}
