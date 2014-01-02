/*
 * Copyright (c) 2013
 */

package model

import reactivemongo.bson.BSONObjectID
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsNull, JsValue}

/**
 * Security token that allows restricted actions via email for limited time.
 */
case class Token(_id: BSONObjectID, userId: BSONObjectID,
    securityKey: String, created: DateTime, expires: DateTime,
    data: JsObject) extends MongoEntity
