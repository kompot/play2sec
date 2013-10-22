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

import play.api.mvc.{SimpleResult, Session, Result}
import com.github.kompot.play2sec.authentication.user.{AuthUserIdentity,
AuthUser}

case class LoginSignupResult(result: Option[SimpleResult] = None,
    url: Option[String] = None, authUser: Option[AuthUser] = None,
    session: Option[Session] = None) {

  def this(result: SimpleResult) = this(Some(result))

  def this(url: String) = this(None, Some(url))

  def this(url: String, session: Session) = this(None, Some(url), None, Some(session))

  def this(authUser: AuthUser) = this(None, None, Some(authUser))
}

