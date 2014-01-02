/*
 * Copyright (c) 2013
 */

package controllers

import play.api.libs.json.{Json, JsString, JsObject, JsValue}
import model.{RemoteUser, User}

trait JsonWebConversions {

  implicit def toJson(jro: JsResponse): JsValue = {
    Json.obj(
      "status" -> JsString(jro.getStatus),
      "message" -> JsString(jro.getMessage),
      "data" -> jro.getData
    )
  }

}
