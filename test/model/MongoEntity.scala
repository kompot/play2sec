/*
 * Copyright (c) 2013
 */

package model

import reactivemongo.bson.BSONObjectID

trait MongoEntity {
  def _id: BSONObjectID
}
