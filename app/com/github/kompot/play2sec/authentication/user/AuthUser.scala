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

  override def toString = s"$id@$provider"
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

  // TODO uncomment?
//  // T extends AuthUserIdentity & NameIdentity
//  def toString[T](identity: T): String = {
//    val sb = new StringBuilder
//    if (identity.getName() != null) {
//      sb.append(identity.getName())
//      sb.append(" ")
//    }
//    if(identity instanceof EmailIdentity) {
//      final EmailIdentity i2 = (EmailIdentity) identity
//      if (i2.getEmail() != null) {
//        sb.append("(")
//        sb.append(i2.getEmail())
//        sb.append(") ")
//      }
//    }
//    if (sb.length() == 0) {
//      sb.append(identity.getId())
//    }
//    sb.append(" @ ")
//    sb.append(identity.getProvider())
//
//    sb.toString
//  }
}
