/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.service

import play.api.mvc.Request
import com.github.kompot.play2sec.authentication.user.{AuthUserIdentity, AuthUser}

import scala.concurrent.Future

trait UserService {
  /**
   * Saves auth provider/id combination to a local user
   * @param authUser
   * @return
   */
  def save(authUser: AuthUser): Option[Any]

  /**
   * Returns the local identifying object if the auth provider/id combination
   * has been linked to a local user account already or null if not.
   * This gets called on any login to check whether the session user still
   * has a valid corresponding local user
   *
   * @param identity
   * @return
   */
  def getByAuthUserIdentitySync(identity: AuthUserIdentity): AnyRef

  /**
   * Merges two user accounts after a login with an auth provider/id that
   * is linked to a different account than the login from before
   * Returns the user to generate the session information from
   *
   * @param newUser
   * @param oldUser
   * @return
   */
  def merge(newUser: AuthUser, oldUser: Option[AuthUser]): AuthUser

  /**
   * Links a new account to an exsting local user.
   * Returns the auth user to log in with
   *
   * @param oldUser
   * @param newUser
   * @return
   */
  def link(oldUser: Option[AuthUser], newUser: AuthUser): AuthUser

  /**
   * Unlinks account from an exsting local user.
   * Returns the auth user to log in with
   *
   * @param currentUser
   * @param provider
   * @return
   */
  def unlink(currentUser: Option[AuthUser], provider: String): Option[AuthUser]

  /**
   * Gets called when a user logs in - you might make profile updates here with
   * data coming from the login provider or bump a last-logged-in date
   *
   * @param knownUser
   * @return
   */
  def whenLogin[A](knownUser: AuthUser, request: Request[A]): AuthUser
}
