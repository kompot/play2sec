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

package com.github.kompot.play2sec.authentication

import play.api.Plugin
import play.api.mvc.{SimpleResult, Call}
import com.github.kompot.play2sec.authentication.exceptions.AuthException
import com.github.kompot.play2sec.authentication.service.UserService

// TODO should be no `Call` in return; only SimpleResults based on request type
trait PlaySecPlugin extends Plugin {
  def userService: UserService

  /**
   * What to do when user is authenticated.
   * `A` type is play.api.mvc.AnyContent
   * TODO: how to express that via type system
   */
  def afterAuth[A](a: A): SimpleResult

  def askMerge: Call

  def askLink: Call

  def afterLogout[A](a: A): SimpleResult

  def userExists[A](a: A): SimpleResult

  def userLoginUnverified[A](a: A): SimpleResult

  def userSignupUnverified[A](a: A): SimpleResult

  def userNotFound[A](a: A): SimpleResult

  def passwordRecoveryRequestSuccessful[A](a: A): SimpleResult

  def auth(provider: String): Call

  def login: Call

  @deprecated("Should not handle application level exceptions", "0.0.2")
  def onException(e: AuthException): Call
}
