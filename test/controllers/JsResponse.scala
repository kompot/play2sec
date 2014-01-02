/*
 * Copyright (c) 2013
 */

package controllers

import play.api.data.Form
import play.api.libs.json._
import play.api.libs.functional.syntax._

abstract class JsResponse(status: String, message: String, data: JsValue = JsString("")) {
  def getStatus = status
  def getMessage = message
  def getData = data
}

case class JsResponseOk(message: String, data: JsValue = JsString(""))
    extends JsResponse(JsonResponseType.ok.toString, message, data)

case class JsResponseError[T](message: String, errors: Option[Form[T]] = None)
    extends JsResponse(errors.map(_.forField("errorType")(_.value.getOrElse(
      JsonResponseType.error.toString))).getOrElse(JsonResponseType.error.toString),
      message, errors.map(_.errorsAsJson).getOrElse(JsString(""))) {
  def this() = this("Unknown error.")
}

object JsonResponseType extends Enumeration {
  val ok = Value
  val error = Value
}
