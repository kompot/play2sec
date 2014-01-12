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

package com.github.kompot.play2sec.authentication.service

import play.api.mvc.Request
import com.github.kompot.play2sec.authentication.user.{AuthUserIdentity, AuthUser}

import scala.concurrent.Future

trait UserService {
  type UserClass

  /**
   * Saves auth provider/id combination to a local user
   * @param authUser
   * @return
   */
  def save(authUser: AuthUser): Future[Option[UserClass]]

  /**
   * Returns the local identifying object if the auth provider/id combination
   * has been linked to a local user account already or null if not.
   * This gets called on any login to check whether the session user still
   * has a valid corresponding local user
   *
   * @param identity
   * @return
   */
  def getByAuthUserIdentity(identity: AuthUserIdentity): Future[Option[UserClass]]

  /**
   * Merges two user accounts after a login with an auth provider/id that
   * is linked to a different account than the login from before
   * Returns the user to generate the session information from
   *
   * @param newUser
   * @param oldUser
   * @return
   */
  def merge(newUser: AuthUser, oldUser: Option[AuthUser]): Future[AuthUser]

  /**
   * Links a new account to an existing local user.
   * Returns the auth user to log in with
   *
   * @param oldUser
   * @param newUser
   * @return
   */
  def link(oldUser: Option[AuthUser], newUser: AuthUser): Future[AuthUser]

  /**
   * Unlinks account from an existing local user.
   * Returns the auth user to log in with
   *
   * @param currentUser
   * @param provider
   * @return
   */
  def unlink(currentUser: Option[AuthUser], provider: String): Future[Option[AuthUser]]

  /**
   * Gets called when a user logs in - you might make profile updates here with
   * data coming from the login provider or bump a last-logged-in date
   *
   * @param knownUser
   * @return
   */
  def whenLogin[A](knownUser: AuthUser, request: Request[A]): AuthUser
}
