/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth2.google

import play.api.libs.json.JsValue
import com.github.kompot.play2sec.authentication.providers.oauth2.BasicOAuth2AuthUser
import com.github.kompot.play2sec.authentication.user._

/**
 * From https://developers.google.com/accounts/docs/OAuth2Login#userinfocall
 */
class GoogleAuthUser(node: JsValue, info: GoogleAuthInfo, state: String)
    extends BasicOAuth2AuthUser(node.\(GoogleAuthUser.Constants.ID).as[Option[String]].getOrElse(""), info, state)
    with ExtendedIdentity with PicturedIdentity with ProfiledIdentity with LocaleIdentity {

  val email =           node.\(GoogleAuthUser.Constants.EMAIL)            .as[Option[String]].getOrElse("")
  val emailIsVerified = node.\(GoogleAuthUser.Constants.EMAIL_IS_VERIFIED).as[Option[Boolean]].getOrElse(false)
  val name =            node.\(GoogleAuthUser.Constants.NAME)             .as[Option[String]].getOrElse("")
  val firstName =       node.\(GoogleAuthUser.Constants.FIRST_NAME)       .as[Option[String]].getOrElse("")
  val lastName =        node.\(GoogleAuthUser.Constants.LAST_NAME)        .as[Option[String]].getOrElse("")
  val picture =         node.\(GoogleAuthUser.Constants.PICTURE)          .as[Option[String]].getOrElse("")
  val gender =          node.\(GoogleAuthUser.Constants.GENDER)           .as[Option[String]].getOrElse("")
  val locale =          node.\(GoogleAuthUser.Constants.LOCALE)           .as[Option[String]].getOrElse("")
  val link =            node.\(GoogleAuthUser.Constants.LINK)             .as[Option[String]].getOrElse("")

  override def getProvider = GoogleAuthProvider.PROVIDER_KEY

  override def getId = node.\(GoogleAuthUser.Constants.ID).as[Option[String]].getOrElse("")
  def getEmail = email
  def isEmailVerified = emailIsVerified
  def getName = name
  def getFirstName = firstName
  def getLastName = lastName
  def getPicture = picture
  def getGender = gender
  def getProfileLink = link
  def getLocale = AuthUser.getLocaleFromString(locale).get
}

object GoogleAuthUser {
  object Constants {
    val ID = "id" // "00000000000000",
    val EMAIL = "email" // "fred.example@gmail.com",
    val EMAIL_IS_VERIFIED = "verified_email" // true,
    val NAME = "name" // "Fred Example",
    val FIRST_NAME = "given_name" // "Fred",
    val LAST_NAME = "family_name" // "Example",
    val PICTURE = "picture" // "https://lh5.googleusercontent.com/-2Sv-4bBMLLA/AAAAAAAAAAI/AAAAAAAAABo/bEG4kI2mG0I/photo.jpg",
    val GENDER = "gender" // "male",
    val LOCALE = "locale" // "en-US"
    val LINK = "link" // "https://plus.google.com/107424373956322297554"
  }
}
