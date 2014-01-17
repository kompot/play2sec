/*
 * Copyright 2012-2014 Joscha Feth, Steve Chaloner, Anton Fedchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.kompot.play2sec

import com.typesafe.plugin._
import play.api.Configuration
import play.api.Logger
import play.api.Play._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import play.i18n.Messages
import scala.Predef.String
import com.github.kompot.play2sec.authentication.service.UserService
import com.github.kompot.play2sec.authentication.user.AuthUser
import com.github.kompot.play2sec.authentication.exceptions.AuthException
import com.github.kompot.play2sec.authentication.providers.password
.{LoginSignupResult, Case}
import com.github.kompot.play2sec.authentication.providers.AuthProvider
import java.util.Date
import scala.concurrent.{Await, ExecutionContext, Future}
import ExecutionContext.Implicits.global

package object authentication {
  private val CFG_ROOT = "play2sec"
  // TODO why using fallback links when we are enforced to use
  // PlaySecPlugin which forces us to define constrains and guides through
  // the interface
  @deprecated("Force constraints via PlaySecPlugin", "0.0.2")
  private val CFG_AFTER_AUTH_FALLBACK = "afterAuthFallback"
  @deprecated("Force constraints via PlaySecPlugin", "0.0.2")
  private val CFG_AFTER_LOGOUT_FALLBACK = "afterLogoutFallback"
  @deprecated("Force constraints via PlaySecPlugin", "0.0.2")
  private val CFG_ACCOUNT_MERGE_ENABLED = "accountMergeEnabled"
  private val CFG_ACCOUNT_AUTO_LINK = "accountAutoLink"
  private val CFG_ACCOUNT_AUTO_MERGE = "accountAutoMerge"

  private val PREFIX       = "p2s-"
  // TODO make this private
  val SESSION_ORIGINAL_URL         = PREFIX + "return-url"
  private val SESSION_USER_KEY     = PREFIX + "user-id"
  private val SESSION_PROVIDER_KEY = PREFIX + "provider-id"
  private val SESSION_EXPIRES_KEY  = PREFIX + "exp"
  private val SESSION_ID_KEY       = PREFIX + "session-id"

  private val REDIRECT_STATUS = 303

  private val MERGE_USER_KEY = "merge-user"
  private val LINK_USER_KEY = "link-user"

  def getUser[A](request: Request[A]): Option[AuthUser] = getUser(request.session)

  def getUserService: UserService = use[PlaySecPlugin].userService

  def link[A](request: Request[A], link: Boolean): Future[SimpleResult] =
    getLinkUser(request.session) match {
      case None =>
        Logger.warn("User to be linked not found.")
        Future.successful(Results.Forbidden("User to be linked not found."))
      case Some(user) =>
        Logger.info("User to be linked is found.")
        removeLinkUser(request.session)
        loginAndRedirect(request, linkOrSignupUser(request, link, user))
    }

  def logout[A](request: Request[A]): SimpleResult = {
    Logger.info("Logging out")
    use[PlaySecPlugin].afterLogout(request.body).withNewSession
  }

  def merge[A](request: Request[A], merge: Boolean): Future[SimpleResult] =
    getMergeUser(request.session) match {
      case None =>
        Logger.warn("User to be merged not found.")
        Future.successful(Results.Forbidden("User to be merged not found."))
      case Some(user) =>
        val loginUser: Future[AuthUser] = if (merge) {
          // User accepted merge, so do it
          getUserService.merge(user, getUser(request.session))
        } else {
          // User declined merge, so log out the old user, and log out with
          // the new one
          Future.successful(user)
        }
        removeMergeUser(request.session)
        loginAndRedirect(request, loginUser)
    }

  def handleAuthentication[A](provider: String, request: Request[A],
      payload: Option[Case] = None): Future[SimpleResult] =
     for {
       auth <- getProvider(provider) match {
         case Some(p) => p.authenticate(request, payload)
         case _ => throw new RuntimeException(s"Unknown provider $provider")
       }
     } yield
       auth match {
         case LoginSignupResult(Some(result), _, _, _) =>
           result
         case LoginSignupResult(_, Some(url), _, Some(session)) =>
           Results.Redirect(url, REDIRECT_STATUS).withSession(session)
         case LoginSignupResult(_, Some(url), _, _) =>
           Results.Redirect(url, REDIRECT_STATUS)
         case LoginSignupResult(_, _, Some(authUser), _) =>
           // TODO blocking
           import scala.concurrent.duration._
           Await.result(processUser(request, authUser), 10.second)
       }

  /**
   * Adapted from:
   * http://stackoverflow.com/questions/6666267/architecture-for-merging-multiple-user-accounts-together
   * 1. The account is     linked to a local account and NO session cookie is present
   * --> Login
   * 2. The account is     linked to a local account and  a session cookie is present
   * --> Merge
   * 3. The account is NOT linked to a local account and NO session cookie is present
   * --> Signup
   * 4. The account is NOT linked to a local account and  a session cookie is present
   * --> Link
   * @param loggedIn
   * @param oldIdentity
   * @param request
   * @param newIdentity
   * @param newUser
   * @param oldUser
   * @tparam A
   * @return
   */
  private def choosePath[A](request: Request[A], loggedIn: Boolean,
      oldIdentity: Option[UserService#UserClass],
      newIdentity: Option[UserService#UserClass],
      oldUser: Option[AuthUser],
      newUser: AuthUser): Future[SimpleResult] = {
    val linked = newIdentity != None
    Logger.info(s"IsLinked: $linked, isLoggedIn: $loggedIn")
    (linked, loggedIn, oldIdentity) match {
      case (_, true, None) =>
        // should fall here if user is in session but has been deleted from storage
        Logger.info("User is logged in but identity is not found. " +
            "Probably session has expired. Will log out.")
        Future.successful(logout(request))
      case (true, false, _) =>
        Logger.info("Performing login.")
        loginAndRedirect(request, Future.successful(newUser))
      case (true, true, _) =>
        Logger.info("Performing merge.")
        if (isAccountMergeEnabled && newIdentity != oldIdentity) {
          if (isAccountAutoMerge) {
            Logger.info("Auto merge is active.")
            loginAndRedirect(request, getUserService.merge(newUser, oldUser))
          } else {
            Logger.info("Auto merge is not active.")
            val upd = storeMergeUser(newUser, request.session)
            Future.successful(Results.Redirect(use[PlaySecPlugin].askMerge).withSession(upd))
          }
        } else {
          Logger.info("Doing nothing, auto merging is not enabled or " +
              "already logged in with this user.")
          // the currently logged in user and the new login belong
          // to the same local user,
          // or Account merge is disabled, so just change the log
          // in to the new user
          loginAndRedirect(request, Future.successful(newUser))
        }
      case (false, false, _) =>
        Logger.info("Performing sign up.")
        loginAndRedirect(request, signupUser(newUser))
      case (false, true, _) =>
        if (isAccountAutoLink) {
          Logger.info(s"Linking additional account.")
          loginAndRedirect(request, getUserService.link(oldUser, newUser))
        } else {
          Logger.info(s"Will not link additional account. Auto linking is disabled.")
          val upd = storeLinkUser(newUser, request.session)
          Future.successful(Results.Redirect(use[PlaySecPlugin].askLink).withSession(upd))
        }
    }
  }

  private def getCacheKey(session: Session, key: String): (String, String) = {
    val id = getPlayAuthSessionId(session)
    (id + "_" + key, id)
  }

  private[play2sec] def getConfiguration: Option[Configuration] = application.configuration.getConfig(CFG_ROOT)

  private def getExpiration(session: Session): Long =
    session.get(SESSION_EXPIRES_KEY).map { x =>
    // TODO: unknown error "value toLong is not a member of String" when using
    // x.toLong
      java.lang.Long.parseLong(x)
    }.getOrElse(AuthUser.NO_EXPIRATION)

  private def getFromCache(session: Session, key: String): Option[Any] = {
    // TODO: do not use play cache, use some pluggable api that can be overriden by user
    play.api.cache.Cache.get(getCacheKey(session, key)._1)
  }

  private def getLinkUser(session: Session): Option[AuthUser] = getUserFromCache(session, LINK_USER_KEY)

  private def getMergeUser(session: Session): Option[AuthUser] =
    getUserFromCache(session, MERGE_USER_KEY)

  // TODO: remove ORIGINAL_URL from session
  private def getOriginalUrl[A](request: Request[A]): Option[String] =
    request.session.get(SESSION_ORIGINAL_URL)

  // TODO session value set is not stored later with the response
  private def getPlayAuthSessionId(session: Session): String =
    session.get(SESSION_ID_KEY).getOrElse(java.util.UUID.randomUUID().toString)

  private def getProvider(providerKey: String) = providers.get(providerKey)

  /**
   * Get the user with which we are logged in - is null
   * if we are not logged in (does NOT check expiration)
   * @param session
   * @return
   */
  private[play2sec] def getUser(session: Session): Option[AuthUser] =
    (session.get(SESSION_PROVIDER_KEY), session.get(SESSION_USER_KEY)) match {
      case (Some(provider), Some(id)) =>
        getProvider(provider) match {
          case Some(p) => Some(p.getSessionAuthUser(id, getExpiration(session)))
          case _       => None
        }
      case _ => None
    }

  private def getUserFromCache(session: Session, key: String): Option[AuthUser] =
    getFromCache(session, key) match {
      case None       => None
      case Some(user) => Some(user.asInstanceOf[AuthUser])
    }

  private def isAccountAutoLink     = getConfiguration.flatMap(_.getBoolean(
    CFG_ACCOUNT_AUTO_LINK)).getOrElse(false)

  private def isAccountAutoMerge    = getConfiguration.flatMap(_.getBoolean(
    CFG_ACCOUNT_AUTO_MERGE)).getOrElse(false)

  private def isAccountMergeEnabled = getConfiguration.flatMap(_.getBoolean(
    CFG_ACCOUNT_MERGE_ENABLED)).getOrElse(false)

  /**
   * Checks if the user is logged in (also checks the expiration).
   * @param session
   * @return
   */
  private[play2sec] def isLoggedIn(session: Session): Boolean = {
    val idAndProviderAreNotEmpty = getUser(session) != None
    val providerIsRegistered = providers.hasProvider(session.get(SESSION_PROVIDER_KEY))
    val validExpirationTime =
      if (session.get(SESSION_EXPIRES_KEY).isDefined) {
        // expiration is set
        val expires = getExpiration(session)
        if (expires != AuthUser.NO_EXPIRATION) {
          // and the session expires after now
          new Date().getTime < expires
        } else {
          true
        }
      } else {
        true
      }
    idAndProviderAreNotEmpty && providerIsRegistered && validExpirationTime
  }

  private def linkOrSignupUser[A](request: Request[A], link: Boolean,
      linkUser: AuthUser): Future[AuthUser] = {
    if (link) {
      // User accepted link - add account to existing local user
      Logger.info("Will be linking.")
      getUserService.link(getUser(request.session), linkUser)
    } else {
      // User declined link - create new user
      Logger.info("Will not be linking.")
      try {
        signupUser(linkUser)
      } catch {
        case e: AuthException => throw e
        //          return Results.InternalServerError(e.getMessage)
      }
    }
  }

  private def loginAndRedirect[A](request: Request[A], loginUser: Future[AuthUser]): Future[SimpleResult] =
    for {
      lu <- loginUser
    } yield {
      val newSession = storeUser(request, lu)
      use[PlaySecPlugin].afterAuth(request.body).withSession(newSession - SESSION_ORIGINAL_URL)
    }

  private def processUser[A](request: Request[A], newUser: AuthUser): Future[SimpleResult] = {
    Logger.info("User identity found." + newUser)
    val oldUser = getUser(request.session)
    val loggedIn = isLoggedIn(request.session)
    for {
      oldIdentity <-
        if (loggedIn) getUserService.getByAuthUserIdentity(oldUser.get)
        else Future.successful(None)
      newIdentity <- getUserService.getByAuthUserIdentity(newUser)
      chosenPath <- choosePath(request, loggedIn, oldIdentity, newIdentity, oldUser, newUser)
    } yield {
      chosenPath
    }
  }

  private[play2sec] def removeFromCache(session: Session, key: String): Option[Any] = {
    val o = getFromCache(session, key)
    val k = getCacheKey(session, key)
    play.api.cache.Cache.remove(k._1)
    o
  }

  private def removeLinkUser(session: Session) =
    removeFromCache(session, LINK_USER_KEY)

  private def removeMergeUser(session: Session): Option[Any] =
    removeFromCache(session, MERGE_USER_KEY)

  @throws(scala.Predef.classOf[AuthException])
  private def signupUser(u: AuthUser): Future[AuthUser] =
    for {
      id <- getUserService.save(u)
    } yield {
      if (id == None) {
        throw new AuthException(Messages.get("playauthenticate.core.exception.singupuser_failed"))
      }
      u
    }

  private[play2sec] def storeInCache(session: Session, key: String, o: AnyRef): Session = {
    val cacheKey = getCacheKey(session, key)
    play.api.cache.Cache.set(cacheKey._1, o)
    session + (SESSION_ID_KEY, cacheKey._2)
  }

  private def storeLinkUser(authUser: AuthUser, session: Session): Session =
  // TODO the cache is not good for this
  // it might get cleared any time
    storeUserInCache(session, LINK_USER_KEY, authUser)

  private def storeMergeUser(authUser: AuthUser, session: Session): Session =
    // TODO the cache is not ideal for this, because it
    // might get cleared any time
    storeUserInCache(session, MERGE_USER_KEY, authUser)

  private def storeUser[A](request: Request[A], authUser: AuthUser): Session = {
    // User logged in once more - wanna make some updates?
    val u: AuthUser = getUserService.whenLogin(authUser, request)

    val withExpiration = u.expires != AuthUser.NO_EXPIRATION

    Logger.info(s"Will be storing user $u with expiration = $withExpiration, ${authUser.expires}")
    val session = request.session +
        (SESSION_USER_KEY, u.id) +
        (SESSION_PROVIDER_KEY, u.provider)
    if (withExpiration) session + (SESSION_EXPIRES_KEY, u.expires.toString)
    else                session -  SESSION_EXPIRES_KEY
  }

  private def storeUserInCache(session: Session, key: String, authUser: AuthUser): Session =
    storeInCache(session, key, authUser)
}
