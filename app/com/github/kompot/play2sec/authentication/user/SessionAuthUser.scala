/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.user

case class SessionAuthUser(id: String, provider: String, exp: Long) extends AuthUser {
  def getId = id

  def getProvider = provider

  override def expires = exp
}
