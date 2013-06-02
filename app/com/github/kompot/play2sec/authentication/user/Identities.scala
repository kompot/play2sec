/*
 * Copyright (c) 2013.
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
