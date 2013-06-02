/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth2.facebook

import play.api.libs.json.JsValue
import com.github.kompot.play2sec.authentication.providers.oauth2.BasicOAuth2AuthUser
import com.github.kompot.play2sec.authentication.user._

class FacebookAuthUser(node: JsValue, info: FacebookAuthInfo, state: String)
  extends BasicOAuth2AuthUser(node.\(FacebookAuthUser.Constants.ID).as[String], info, state) with ExtendedIdentity
  with PicturedIdentity with ProfiledIdentity with LocaleIdentity {

  import FacebookAuthUser.Constants._

  val name = node.\(NAME).as[Option[String]].getOrElse("")
  val firstName = node.\(FIRST_NAME).as[Option[String]].getOrElse("")
  val lastName = node.\(LAST_NAME).as[Option[String]].getOrElse("")
  val link = node.\(LINK).as[Option[String]].getOrElse("")
  val username = node.\(USERNAME).as[Option[String]].getOrElse("")
  val gender = node.\(GENDER).as[Option[String]].getOrElse("")
  val email = node.\(EMAIL).as[Option[String]].getOrElse("")
  val verified = node.\(VERIFIED).as[Option[Boolean]].getOrElse(false)
  val timeZone = node.\(TIME_ZONE).as[Option[Int]].getOrElse(0)
  val locale = node.\(LOCALE).as[Option[String]].getOrElse("")
  val updateTime = node.\(UPDATE_TIME).as[Option[String]].getOrElse("")

  override def getProvider = FacebookAuthProvider.PROVIDER_KEY

  override def getName = name
  override def getFirstName = firstName
  override def getLastName = lastName
  override def getProfileLink = link
  def getUsername = username
  override def getGender = gender
  override def getEmail = email
  def isVerified = verified
  def getTimeZone = timeZone
  // According to https://developers.facebook.com/docs/reference/api/#pictures
  override def getPicture = s"https://graph.facebook.com/$username/picture"
  override def getLocale = AuthUser.getLocaleFromString(locale).get
  def getUpdateTime = updateTime
}

object FacebookAuthUser {
  object Constants {
    val ID = "id" // "616473731"
    val NAME = "name" // "Joscha Feth"
    val FIRST_NAME = "first_name"// "Joscha"
    val LAST_NAME = "last_name" // "Feth"
    val LINK = "link" // "http://www.facebook.com/joscha.feth"
    val USERNAME = "username"// "joscha.feth"
    val GENDER = "gender"// "male"
    val EMAIL = "email"// "joscha@feth.com"
    val TIME_ZONE = "timezone"// 2
    val LOCALE = "locale"// "de_DE"
    val VERIFIED = "verified"// true
    val UPDATE_TIME = "updated_time" // "2012-04-26T20:22:52+0000"}
  }
}
