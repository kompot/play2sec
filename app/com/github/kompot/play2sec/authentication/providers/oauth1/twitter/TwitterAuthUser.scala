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

package com.github.kompot.play2sec.authentication.providers.oauth1.twitter

import play.api.libs.json.JsValue
import com.github.kompot.play2sec.authentication.providers.oauth1
.{BasicOAuth1AuthUser, OAuth1AuthInfo}
import com.github.kompot.play2sec.authentication.user.{AuthUser,
LocaleIdentity, PicturedIdentity}
import play.api.Logger

class TwitterAuthUser(node: JsValue, info: OAuth1AuthInfo)
    extends BasicOAuth1AuthUser(node.\(TwitterAuthUser.Constants.ID).as[Option[Int]].getOrElse(0).toString, info, null)
    with PicturedIdentity with LocaleIdentity {
  import TwitterAuthUser._

  // TODO right now {"errors":[{"message":"The Twitter REST API v1 is no longer
  // active. Please migrate to API v1.1. https://dev.twitter.com/docs/api/1.1/
  // overview.","code":68}]} is returned
//  Logger.warn("TwitterAuthUser node" + node)

  // TODO: how to get ID that is set in constructor?
  override def getId = node.\(TwitterAuthUser.Constants.ID).as[Option[Int]].getOrElse(0).toString
  override def getProvider = TwitterAuthProvider.PROVIDER_KEY
  override def getName = node.\(Constants.NAME).as[Option[String]].getOrElse("")
  def getScreenName = node.\(Constants.SCREEN_NAME).as[Option[String]].getOrElse("")
  def isVerified = node.\(Constants.VERIFIED).as[Option[Boolean]].getOrElse(false)
  override def getPicture = node.\(Constants.PROFILE_IMAGE_URL).as[Option[String]].getOrElse("")

  override def getLocale = AuthUser.getLocaleFromString(node.\(Constants.LOCALE).as[Option[String]].getOrElse("")).get
}

object TwitterAuthUser {
  object Constants {
    // {
    val ID = "id"
    // "id":15484335,
    // "listed_count":5,
    val PROFILE_IMAGE_URL = "profile_image_url"
    // "profile_image_url":"http://a0.twimg.com/profile_images/57096786/j_48x48_normal.png",
    // "following":false,
    // "followers_count":118,
    // "location":"Sydney, Australia",
    // "contributors_enabled":false,
    // "profile_background_color":"C0DEED",
    // "time_zone":"Berlin",
    // "geo_enabled":true,
    // "utc_offset":3600,
    // "is_translator":false,
    val NAME = "name"
    // "name":"Joscha Feth",
    // "profile_background_image_url":"http://a0.twimg.com/images/themes/theme1/bg.png",
    // "show_all_inline_media":false,
    val SCREEN_NAME = "screen_name"
    // "screen_name":"joschafeth",
    // "protected":false,
    // "profile_link_color":"0084B4",
    // "default_profile_image":false,
    // "follow_request_sent":false,
    // "profile_background_image_url_https":"https://si0.twimg.com/images/themes/theme1/bg.png",
    // "favourites_count":3,
    // "notifications":false,
    val VERIFIED = "verified"
    // "verified":false,
    // "profile_use_background_image":true,
    // "profile_text_color":"333333",
    // "description":"",
    // "id_str":"15484335",
    val LOCALE = "lang"
    // "lang":"en",
    // "profile_sidebar_border_color":"C0DEED",
    // "profile_image_url_https":"https://si0.twimg.com/profile_images/57096786/j_48x48_normal.png",
    // "default_profile":true,
    // "url":null,
    // "statuses_count":378,
    // "status":{
    // "in_reply_to_user_id":11111,
    // "truncated":false,
    // "created_at":"Mon Jul 23 13:22:31 +0000 2012",
    // "coordinates":null,
    // "geo":null,
    // "favorited":false,
    // "in_reply_to_screen_name":"XXX",
    // "contributors":null,
    // "in_reply_to_status_id_str":"111111",
    // "place":null,
    // "source":"<a href=\"http://itunes.apple.com/us/app/twitter/id409789998?mt=12\" rel=\"nofollow\">Twitter for Mac</a>",
    // "in_reply_to_user_id_str":"11111",
    // "id":111111,
    // "id_str":"111111",
    // "retweeted":false,
    // "retweet_count":0,
    // "in_reply_to_status_id":11111,
    // "text":"some text to up to 140chars here"
    // },
    // "profile_background_tile":false,
    // "friends_count":120,
    // "created_at":"Fri Jul 18 18:17:46 +0000 2008",
    // "profile_sidebar_fill_color":"DDEEF6"
  }
}
