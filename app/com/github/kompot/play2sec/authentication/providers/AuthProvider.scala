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

package com.github.kompot.play2sec.authentication.providers

import play.api.{Configuration, Plugin}
import play.api.mvc.{Request, AnyContent}
import play.api.Play.current
import com.typesafe.plugin._
import com.github.kompot.play2sec.authentication.PlaySecPlugin
import com.github.kompot.play2sec.authentication.exceptions.AuthException
import com.github.kompot.play2sec.authentication.providers.password
.{LoginSignupResult, Case}
import com.github.kompot.play2sec.authentication.user.{SessionAuthUser,
AuthUser}
import scala.concurrent.Future
import com.github.kompot.play2sec.authentication

abstract case class AuthProvider(app: play.api.Application) extends Plugin {
  override def onStart() {
    val settings = requiredSettings
    if (settings.size > 0) {
      providerConfigOption match {
        case None =>
          throw new RuntimeException(s"No settings for provider '$key' available at all!")
        case Some(c) =>
          for (key <- settings) {
            val setting = c.getString(key)
            if (setting == None) {
              throw new RuntimeException(s"Provider '$key' missing needed setting '$key'.")
            }
          }
      }
    }
    register(key, this)
  }


  override def onStop() {
    unregister(key)
  }

  // TODO is it used
  def getUrl: String = use[PlaySecPlugin].auth(key).url

  // TODO is it used
  def getAbsoluteUrl(request: Request[AnyContent]): String = {
    use[PlaySecPlugin]
        .auth(key)
        // TODO: determine whether secure or not in runtime
        .absoluteURL(secure = false)(request)
  }

  val key: String

  protected def providerConfigOption: Option[Configuration] =
    authentication.getConfiguration.flatMap(_.getConfig(key))

  protected def providerConfig: Configuration =
    providerConfigOption.get

  /**
   * Returns either an AuthUser object or a String (URL)
   *
   * @param request
   * @param payload
   * Some arbitrary payload that shall get passed into the
   * authentication process
   * @return
   * @throws AuthException
   */
  @throws(classOf[AuthException])
  def authenticate[A](request: Request[A], payload: Option[Case]): Future[LoginSignupResult]

  /**
   * Mandatory settings that must be supplied in order for auth provider to work.
   * @return
   */
  protected def requiredSettings: List[String]

  def getSessionAuthUser(id: String, expires: Long): AuthUser =
    new SessionAuthUser(id, key, expires)

  // TODO what is it for?
  val isExternal: Boolean
}
