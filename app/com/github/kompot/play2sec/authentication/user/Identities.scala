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
  def getId: String
  def getProvider: String
}
trait EmailIdentity extends AuthUserIdentity { def getEmail: String }
trait NameIdentity { def getName: String }
trait BasicIdentity extends EmailIdentity with NameIdentity
trait ExtendedIdentity extends BasicIdentity {
  def getFirstName: String
  def getLastName: String
  def getGender: String
}
trait PicturedIdentity { def getPicture: String }
trait ProfiledIdentity { def getProfileLink: String }
trait LocaleIdentity { def getLocale: Locale }
