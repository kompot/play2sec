/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.password

case class DefaultUsernamePasswordAuthUser(clearPassword: String, email: String)
    extends UsernamePasswordAuthUser(clearPassword, email) {
//  override def getId = getEmail

  /**
   * This MUST be overwritten by an extending class.
   * The default implementation stores a clear string, which is NOT recommended.
   *
   * Should return null if the clearString given is null.
   *
   * @param clearString
   * @return
   */
//  override protected def createPassword(clearString: String) = clearString
}
