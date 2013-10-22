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

trait AuthUserIdentity {
  def id: String
  def provider: String

  override def hashCode =
    41 * (41 + id.hashCode) + provider.hashCode

  override def equals(other: Any) = other match {
    case that: AuthUserIdentity =>
      (that canEqual this) &&
      this.id == that.id &&
      this.provider == that.provider
    case _ => false
  }

  def canEqual(other: Any) = other.isInstanceOf[AuthUserIdentity]
}
trait EmailIdentity extends AuthUserIdentity { def email: String }
trait NameIdentity { def name: String }
trait BasicIdentity extends EmailIdentity with NameIdentity
trait ExtendedIdentity extends BasicIdentity {
  def firstName: String
  def lastName: String
  def gender: String
}
trait PicturedIdentity { def picture: String }
trait ProfiledIdentity { def profileLink: String }
trait LocaleIdentity { def locale: Locale }
