/*
 * Copyright (c) 2013
 */

package model

import com.github.kompot.play2sec.authentication.user.{ExtendedIdentity,
AuthUserIdentity, AuthUser}
import scala.concurrent.Future
import com.github.kompot.play2sec.authentication.providers.password
.{UsernamePasswordAuthProvider, UsernamePasswordAuthUser}
import play.api.mvc.Request
import com.github.kompot.play2sec.authentication.providers
.MyUsernamePasswordAuthUser
import scala.Some
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import model.MongoConversions._
import scala.concurrent.ExecutionContext.Implicits.global

class UserService extends com.github.kompot.play2sec.authentication.service.UserService
    with MongoService {
  type A = User

  type UserClass = User

  protected val collectionName = "user"

  protected val bsonDocumentHandler = userHandler

  def getByUsernameOrId(usernameOrId: String): Future[Option[User]] =
    if (isUsernameOrIdIsId(usernameOrId))
      get(BSONObjectID(usernameOrId))
    else
      queryOne(BSONDocument("username" -> usernameOrId))

  private def isUsernameOrIdIsId(usernameOrId: String): Boolean =
    "[A-Fa-f0-9]{24}".r.findFirstIn(usernameOrId).isDefined

  def getByAuthUserIdentity(authUser: Option[AuthUserIdentity]): Future[Option[User]] = {
    if (authUser.isDefined) getByAuthUserIdentity(authUser.get) else Future[Option[User]](None)
  }

  def getByAuthUserIdentity(authUser: AuthUserIdentity): Future[Option[User]] = {
    queryOne(BSONDocument(
      "remoteUsers.provider" -> authUserProviderToRemoteUserProvider(authUser).toString,
      "remoteUsers.id" -> authUser.id
    ))
  }

  def existsByAuthUserIdentity(authUser: AuthUser): Future[Boolean] = {
    queryOne(BSONDocument(
      "remoteUsers.provider" -> authUserProviderToRemoteUserProvider(authUser).toString,
      "remoteUsers.id" -> authUser.id
    )).map(!_.isEmpty)
  }

  // TODO remove from sample
  @deprecated
  def usernameIsUnique(username: String, currentUser: Option[User]): Future[Boolean] = {
    // TODO: check case insensitive
    queryOne(BSONDocument("username" -> username)).map(_.filter(u =>
      currentUser.exists(_.username != u.username)).isEmpty)
  }

  def checkLoginAndPassword(email: String, password: String): Future[Boolean] = {
    val loginUser = new MyUsernamePasswordAuthUser(password, email)
    for {
      dbu <- getByAuthUserIdentity(loginUser)
    } yield {
      dbu.exists(user => loginUser.checkPassword(user.password, password))
    }
  }

  def emailExists(email: String): Future[Boolean] =
    existsByAuthUserIdentity(new MyUsernamePasswordAuthUser("", email))

  def createByAuthUserIdentity(authUser: AuthUser): User = {
    val remoteUser = new RemoteUser(authUserProviderToRemoteUserProvider(authUser).toString, authUser.id)
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

    val user = new User(BSONObjectID.generate, None, Some(pass), nameLast,
      nameFirst, Set(remoteUser))
    // could not afford async - we must be sure that user has been created
    MongoWait(insert(user))
    user
  }

  def authUserProviderToRemoteUserProvider(authUser: AuthUserIdentity): RemoteUserProvider.Value = {
    RemoteUserProvider.withName(authUser.provider)
  }

  def showUsers() = query()

  def save(authUser: AuthUser) = {
    for {
      existing <- existsByAuthUserIdentity(authUser)
    } yield {
      if (!existing)
        Some(createByAuthUserIdentity(authUser))
      else
        None
    }
  }

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
          val newRu = RemoteUser(newUser.provider, newUser.id, isConfirmed = true)
          collection.update(
            BSONDocument("_id" -> u.get._id),
            BSONDocument("$push" ->
                BSONDocument("remoteUsers" -> remoteUserHandler.write(newRu))
            )
          )
          old
        }
      }
    }
  }

  def whenLogin[A](knownUser: AuthUser, request: Request[A]): AuthUser = knownUser

  def verifyEmail(userId: BSONObjectID, email: String): Future[Boolean] = {
    collection.update(
      BSONDocument(
        "_id" -> userId,
        "remoteUsers.provider" -> UsernamePasswordAuthProvider.PROVIDER_KEY,
        "remoteUsers.id" -> email,
        "remoteUsers.isConfirmed" -> false
      ),
      BSONDocument("$set" -> BSONDocument("remoteUsers.$.isConfirmed" -> true)),
      writeConcern = awaitJournalCommit
    ).map{ res =>
      // TODO: return real result, make use of MongoDB updated documents count
      // right now it will return true even when email is forged
      res.ok
    }
  }

  /**
   * Detach account from an existing local user.
   * Returns the auth user to log in with
   */
  def unlink(currentUser: Option[AuthUser], provider: String) = {
    for {
      user <- getByAuthUserIdentity(currentUser.get)
    } yield {
      collection.update(
        BSONDocument("_id" -> user.get._id),
        BSONDocument("$pull" ->
            BSONDocument("remoteUsers" -> BSONDocument("provider" -> provider))),
        awaitJournalCommit
      )
      currentUser
    }
  }
}
