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

package com.github.kompot.play2sec.authentication.user

import java.util.Locale
import org.apache.commons.lang3.LocaleUtils

trait AuthUser extends AuthUserIdentity {
  def expires = AuthUser.NO_EXPIRATION
  
  def confirmedRightAway = false

  override def toString = AuthUser.toString(this)
}

object AuthUser {
  val NO_EXPIRATION = -1L

  def getLocaleFromString(locale: String): Option[Locale] = {
    if (!locale.isEmpty) {
      try {
        Some(LocaleUtils.toLocale(locale))
      } catch { case e: java.lang.IllegalArgumentException =>
        try {
          Some(LocaleUtils.toLocale(locale.replace('-', '_')))
        } catch { case e1: java.lang.IllegalArgumentException =>
          None
        }
      }
    } else {
      None
    }
  }

  def toString[T <: AuthUserIdentity](identity: T): String = {
    val sb = new StringBuilder
    identity match {
      case i2: NameIdentity =>
        if (i2.name != null) {
          sb.append(i2.name)
          sb.append(" ")
        }
      case _ =>
    }
    identity match {
      case i2: EmailIdentity =>
        if (i2.email != null) {
          sb.append("(")
          sb.append(i2.email)
          sb.append(") ")
        }
      case _ =>
    }
    if (sb.length == 0) {
      sb.append(identity.id)
    }
    sb.append(" @ ")
    sb.append(identity.provider)

    sb.toString()
  }
}
