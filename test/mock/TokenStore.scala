package mock

import model.Token
import com.github.kompot.play2sec.authentication.user.AuthUser
import play.api.libs.json.JsObject
import scala.concurrent.{ExecutionContext, Future}
import org.joda.time.DateTime
import ExecutionContext.Implicits.global

class TokenStore(userStore: UserStore) extends KvStore {
  type A = Token

  def generateToken(user: AuthUser, data: JsObject): Future[Token] =
    for (
      user <- userStore.getByAuthUserIdentity(user)
    ) yield {
      user.map { u =>
        val token = Token(generateId, u._id, generateId, DateTime.now, data)
        put(token._id, token)
        token
      } getOrElse {
        throw new IllegalArgumentException(s"User not found by AuthUser $user")
      }
    }

  def getValidTokenBySecurityKey(s: String): Future[Option[Token]] =
    Future.successful(store.find(_._2.securityKey == s).map(_._2))

}
