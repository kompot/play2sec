/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.providers.oauth2.vkontakte

import play.api.libs.json.JsValue
import com.github.kompot.play2sec.authentication.providers.oauth2
.BasicOAuth2AuthUser
import com.github.kompot.play2sec.authentication.user.AuthUserIdentity

class VkontakteAuthUser(node: JsValue, info: VkontakteAuthInfo, state: String)
    extends BasicOAuth2AuthUser(node.\("response")(0).\(VkontakteAuthUser.Constants.ID).as[Option[Int]].getOrElse(0).toString, info, state)
    with AuthUserIdentity {

  val firstName = node.\("response")(0).\(VkontakteAuthUser.Constants.FIRST_NAME).as[Option[String]].getOrElse("")
  val lastName =  node.\("response")(0).\(VkontakteAuthUser.Constants.LAST_NAME) .as[Option[String]].getOrElse("")

  override def getProvider = VkontakteAuthProvider.PROVIDER_KEY

  override def getId = node.\("response")(0).\(VkontakteAuthUser.Constants.ID).as[Option[Int]].getOrElse(0).toString
  def getEmail = ""
	def getName = lastName + " " + firstName
	def getFirstName = firstName
  def getLastName = lastName
}

object VkontakteAuthUser {
  object Constants {
    val ID = "uid" // "00000000000000",
    val FIRST_NAME = "first_name" // "Fred",
    val LAST_NAME = "last_name" // "Example",
  }
}
