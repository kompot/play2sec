/*
 * Copyright (c) 2013
 */

package model

import org.joda.time.DateTime
import play.api.libs.json.JsObject

/**
 * Security token that allows restricted actions via email.
 */
case class Token(_id: String, userId: String,
    securityKey: String, created: DateTime, data: JsObject)
