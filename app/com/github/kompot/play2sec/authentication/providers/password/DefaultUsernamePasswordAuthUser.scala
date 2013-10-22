/*
 * Copyright 2012-2013 Joscha Feth, Steve Chaloner, Anton Fedchenko
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
