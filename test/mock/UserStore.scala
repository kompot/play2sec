package mock

import com.github.kompot.play2sec.authentication.service.UserService
import model.RemoteUserProvider
import com.github.kompot.play2sec.authentication.user.{ExtendedIdentity,
AuthUserIdentity, AuthUser}
import play.api.mvc.Request
import scala.concurrent.{ExecutionContext, Future}
import com.github.kompot.play2sec.authentication.providers.password
.{UsernamePasswordAuthProvider, UsernamePasswordAuthUser}
import model.User
import model.RemoteUser
import scala.Some
import ExecutionContext.Implicits.global
import com.github.kompot.play2sec.authentication.providers
.MyUsernamePasswordAuthUser

class UserStore extends KvStore with UserService {
  type A = User

  type UserClass = User

  def save(authUser: AuthUser) =
    if (!existsByAuthUserIdentity(authUser))
      Future.successful(Some(createByAuthUserIdentity(authUser)))
    else
      Future.successful(None)

  def getByAuthUserIdentity(authUser: AuthUserIdentity) =
    Future.successful(store.find(u => u._2.remoteUsers.exists(ru =>
      ru.id       == authUser.id &&
      ru.provider == authUserProviderToRemoteUserProvider(authUser).toString) && !u._2.isBlocked).map(_._2))

  def merge(newUser: AuthUser, oldUser: Option[AuthUser]) = {
    link(oldUser, newUser)
    Future.successful(newUser)
  }

  def link(oldUser: Option[AuthUser], newUser: AuthUser) = {
    oldUser match {
      case None => Future.successful(newUser)
      case Some(old) => {
        for {
          u <- getByAuthUserIdentity(old)
        } yield {
          val newRu = RemoteUser(newUser.provider, newUser.id,
            isConfirmed = newUser.confirmedRightAway)
          val updUser = u.get.copy(remoteUsers = (u.get.remoteUsers + newRu).toSet)
          put(updUser._id, updUser)
          old
        }
      }
    }
  }

  def unlink(currentUser: Option[AuthUser], provider: String) = {
    for {
      user <- getByAuthUserIdentity(currentUser.get)
    } yield {
      val updUser = user.get.copy(remoteUsers = user.get.remoteUsers.filterNot(_.provider == provider))
      put(updUser._id, updUser)
      currentUser
    }
  }

  def whenLogin[A](knownUser: AuthUser, request: Request[A]) = knownUser

  def emailExists(email: String): Future[Boolean] =
    Future.successful(existsByAuthUserIdentity(new MyUsernamePasswordAuthUser("", email)))

  def checkLoginAndPassword(email: String, password: String): Future[Boolean] = {
    val loginUser = new MyUsernamePasswordAuthUser(password, email)
    for {
      dbu <- getByAuthUserIdentity(loginUser)
    } yield {
      dbu.exists(user => loginUser.checkPassword(user.password, password))
    }
  }

  def verifyEmail(userId: String, email: String): Future[Boolean] = {
    val user = store.find(_._1 == userId).map(_._2)
    val unconfirmedEmailUser = user.flatMap(_.remoteUsers.find(ru =>
      ru.provider == UsernamePasswordAuthProvider.PROVIDER_KEY &&
      ru.id == email &&
      !ru.isConfirmed))
    if (unconfirmedEmailUser.isDefined) {
      val updatedRemoteUsers = (user.get.remoteUsers.filterNot(_ == unconfirmedEmailUser.get) +
          unconfirmedEmailUser.get.copy(isConfirmed = true)).toSet
      val newUser = user.get.copy(remoteUsers = updatedRemoteUsers)
      put(newUser._id, newUser)
      Future.successful(true)
    } else {
      Future.successful(false)
    }
  }

  /**
   * Similar to getByAuthUserIdentity but does not check whether User is blocked.
   * @param authUser
   * @return
   */
  private def existsByAuthUserIdentity(authUser: AuthUser): Boolean =
    store.exists(_._2.remoteUsers.exists(ru =>
      ru.id       == authUser.id &&
      ru.provider == authUserProviderToRemoteUserProvider(authUser).toString))

  private def authUserProviderToRemoteUserProvider(authUser: AuthUserIdentity): RemoteUserProvider.Value = {
    RemoteUserProvider.withName(authUser.provider)
  }

  private def createByAuthUserIdentity(authUser: AuthUser): User = {
    val remoteUser = new RemoteUser(authUserProviderToRemoteUserProvider(authUser).toString,
      authUser.id, isConfirmed = authUser.confirmedRightAway)
    val pass = authUser match {
      case user: UsernamePasswordAuthUser => user.getHashedPassword
      case _ => ""
    }
    val nameLast = authUser match {
      case identity: ExtendedIdentity => identity.lastName
      case _ => ""
    }
    val nameFirst = authUser match {
      case identity: ExtendedIdentity => identity.firstName
      case _ => ""
    }

    val user = new User(generateId, None, Some(pass), nameLast,
      nameFirst, Set(remoteUser))
    put(user._id, user)
    user
  }
}
