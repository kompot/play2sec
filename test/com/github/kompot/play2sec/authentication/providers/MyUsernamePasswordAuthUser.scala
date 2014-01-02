/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authentication.providers

import com.github.kompot.play2sec.authentication.providers.password
.UsernamePasswordAuthUser
import com.github.kompot.play2sec.authentication.user.NameIdentity

case class MyUsernamePasswordAuthUser(password: String, email: String)
    extends UsernamePasswordAuthUser(password, email) with NameIdentity {
  def this(password: String) = this(password, "")
  def name = ""
}
