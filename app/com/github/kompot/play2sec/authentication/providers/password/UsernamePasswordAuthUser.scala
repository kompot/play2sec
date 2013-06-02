/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.password

import org.mindrot.jbcrypt.BCrypt
import play.api.Logger
import com.github.kompot.play2sec.authentication.user.{EmailIdentity, AuthUser}

abstract class UsernamePasswordAuthUser(clearPassword: String, email: String) extends AuthUser with EmailIdentity {
  override def getEmail = email
  override def getId = email
  override def getProvider = UsernamePasswordAuthProvider.PROVIDER_KEY

  def getHashedPassword: String = {
    createPassword(clearPassword)
  }

  /**
   * You *SHOULD* provide your own implementation of this which implements your own security.
   */
  protected def createPassword(clearString: String): String = {
    BCrypt.hashpw(clearString, BCrypt.gensalt())
  }

  /**
   * You *SHOULD* provide your own implementation of this which implements your own security.
   */
  def checkPassword(hashed: Option[String], candidate: String): Boolean = {
    // TODO: remove null checking?
    if (hashed == null || candidate == null || hashed == None) {
      false
    } else {
      Logger.debug(s"hashed $hashed candidate $candidate")
      BCrypt.checkpw(candidate, hashed.get)
    }
  }
}
