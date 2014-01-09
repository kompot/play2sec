package mock

import scala.collection.parallel.mutable._
import org.mindrot.jbcrypt.BCrypt
import scala.concurrent.Future

trait KvStore {
  type A

  protected val store: ParMap[String, A] = new ParHashMap[String, A]

  protected def generateId = KvStore.generateId

  def put(k: String, v: A): String = {
    store.put(k, v)
    k
  }

  def get(k: String): Future[Option[A]] = Future.successful(store.get(k))

  def clearStore() = store.clear()

  def getStore = store
}

object KvStore {
  def generateId = BCrypt.gensalt().replaceAll("[^A-Za-z0-9]", "")
}
