/*
 * Copyright (c) 2013
 */

package model

import scala.Some
import play.api.libs.json._
import play.api.libs.functional._
import reactivemongo.bson._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import org.joda.time.DateTime

object MongoConversions {
  implicit object DateTimeBSONWriter extends BSONWriter[DateTime, BSONDateTime] {
    def write(t: DateTime): BSONDateTime = BSONDateTime(t.getMillis)
  }

  implicit object DateTimeBSONReader extends BSONReader[BSONDateTime, DateTime] {
    def read(b: BSONDateTime): DateTime = new DateTime(b.value)
  }

  implicit val remoteUserHandler = Macros.handler[RemoteUser]
  implicit val userHandler = Macros.handler[User]
  implicit val tokenHandler = Macros.handler[Token]
}
