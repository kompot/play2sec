/*
 * Copyright (c) 2013
 */

package model

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import reactivemongo.core.commands.Count
import reactivemongo.core.commands.GetLastError
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.ReactiveMongoPlugin
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.modules.reactivemongo.json.ImplicitBSONHandlers
import reactivemongo.core.commands.GetLastError
import scala.Some
import reactivemongo.api.collections.default.BSONCollection

trait MongoService extends ImplicitBSONHandlers {
  type A <: MongoEntity

  protected val db =  ReactiveMongoPlugin.db

  protected val collectionName: String

  protected val awaitJournalCommit: GetLastError = GetLastError(j = true)

  implicit protected val bsonDocumentHandler: BSONDocumentReader[A] with BSONDocumentWriter[A] with BSONHandler[BSONDocument, A]

  def insert(a: A): Future[LastError] = collection.insert(a, awaitJournalCommit)

  def update(a: A): Future[LastError] = collection.update(BSONDocument("_id" -> a._id), a, awaitJournalCommit, upsert = false)

  def get(id: BSONObjectID): Future[Option[A]] = collection.find(BSONDocument("_id" -> id)).one[A]

  def get(id: Option[BSONObjectID]): Future[Option[A]] = id match {
    case Some(i) => get(i)
    case None    => Future.successful(None)
  }

  def exists(id: BSONObjectID): Future[Boolean] = get(id).map(_.isDefined)

  def getByIds(ids: Array[BSONObjectID]): Future[List[A]] =
    collection.find(BSONDocument("_id" -> BSONDocument("$in" -> ids))).cursor[A].collect[List](1000, stopOnError = false)

  def query(doc: BSONDocument = BSONDocument()): Future[List[A]] =
    collection.find(doc).cursor[A].collect[List](1000, stopOnError = false)

  def queryOne(doc: BSONDocument = BSONDocument()): Future[Option[A]] = collection.find(doc).one[A]

  def remove(id: BSONObjectID) = collection.remove(BSONDocument("_id" -> id))

  def countAll() = db.command(Count(collectionName, None))

  def removeAll() = collection.remove(BSONDocument())

  // TODO could be made val if collection name is somehow known
  protected def collection = db[BSONCollection](collectionName)
}
