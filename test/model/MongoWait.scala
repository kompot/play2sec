package model

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object MongoWait {
  val defaultTimeout = 10.second

  /**
   * Use synchronous data retrieval only when absolutely required.
   * E. g. to check web forms for errors. Better find a way to avoid using this.
   */
  @deprecated("Make use of Scala futures.", "0.0.5")
  def apply[T](future: Future[T]): T = Await.result(future, defaultTimeout)
}
