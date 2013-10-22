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

package com.github.kompot.play2sec.authentication

import play.api.mvc.Call
import com.github.kompot.play2sec.authentication.exceptions.AuthException

@deprecated("Was used instead of plugin?", "0.0.1")
abstract class Resolver {

  /**
   * This is the route to your login page
   *
   * @return
   */
  def login: Call

  /**
   * Route to redirect to after authentication has been finished.
   * Only used if no original URL was stored.
   * If you return null here, the user will get redirected to the URL of
   * the setting
   * afterAuthFallback
   * You can use this to redirect to an external URL for example.
   *
   * @return
   */
  def afterAuth: Call

  /**
   * This should usually point to the route where you registered
   * com.feth.play.module.pa.controllers.AuthenticateController.
   * authenticate(String)
   * however you might provide your own authentication implementation if
   * you want to
   * and point it there
   *
   * @param provider
   *            The provider ID matching one of your registered providers
   *            in play.plugins
   *
   * @return a Call to follow
   */
  def auth(provider: String): Call

  /**
   * If you set the accountAutoMerge setting to true, you might return
   * null for this.
   *
   * @return
   */
  def askMerge: Call

  /**
   * If you set the accountAutoLink setting to true, you might return null
   * for this
   *
   * @return
   */
  def askLink: Call

  /**
   * Route to redirect to after logout has been finished.
   * If you return null here, the user will get redirected to the URL of
   * the setting
   * afterLogoutFallback
   * You can use this to redirect to an external URL for example.
   *
   * @return
   */
  def afterLogout: Call

  def onException(e: AuthException)
}
