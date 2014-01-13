/*
 * Copyright 2012-2014 Joscha Feth, Steve Chaloner, Anton Fedchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.kompot.play2sec.authentication.providers.password

import org.mindrot.jbcrypt.BCrypt
import play.api.Logger
import com.github.kompot.play2sec.authentication.user.{EmailIdentity, AuthUser}

abstract class UsernamePasswordAuthUser(clearPassword: String, email: String)
    extends AuthUser with EmailIdentity {
  override def id = email
  override def provider = UsernamePasswordAuthProvider.PROVIDER_KEY

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
    Logger.info(s"Checking password; hashed $hashed candidate $candidate")
    hashed.exists(BCrypt.checkpw(candidate, _))
  }
}
