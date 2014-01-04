/*
 * Copyright 2012-2013 Joscha Feth, Steve Chaloner, Anton Fedchenko
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
  private val CFG_AFTER_AUTH_FALLBACK = "afterAuthFallback"
  private val CFG_AFTER_LOGOUT_FALLBACK = "afterLogoutFallback"
  private val CFG_ACCOUNT_MERGE_ENABLED = "accountMergeEnabled"
  private val CFG_ACCOUNT_AUTO_LINK = "accountAutoLink"
  private val CFG_ACCOUNT_AUTO_MERGE = "accountAutoMerge"

  private val SESSION_PREFIX       = "p2s-"
  // TODO make this private
  val SESSION_ORIGINAL_URL         = SESSION_PREFIX + "return-url"
  private val SESSION_USER_KEY     = SESSION_PREFIX + "user-id"
  private val SESSION_PROVIDER_KEY = SESSION_PREFIX + "provider-id"
  private val SESSION_EXPIRES_KEY  = SESSION_PREFIX + "exp"
  private val SESSION_ID_KEY       = SESSION_PREFIX + "session-id"

  private val REDIRECT_STATUS = 303

  // TODO null? what is it for?
  private val MERGE_USER_KEY: String = null
  // TODO null what is it for?
  private val LINK_USER_KEY: String = null

  def getConfiguration: Option[Configuration] = application.configuration.getConfig(CFG_ROOT)

  // TODO: remove ORIGINAL_URL from session
  private def getOriginalUrl[A](request: Request[A]): Option[String] =
    request.session.get(SESSION_ORIGINAL_URL)

  def getUserService: UserService = use[PlaySecPlugin].userService

  def storeUser[A](request: Request[A], authUser: AuthUser): Session = {
    // User logged in once more - wanna make some updates?
    val u: AuthUser = getUserService.whenLogin(authUser, request)

    val withExpiration = u.expires != AuthUser.NO_EXPIRATION

    Logger.info("Will be storing user " + u)
    val session = request.session +
        (SESSION_USER_KEY, u.id) +
        (SESSION_PROVIDER_KEY, u.provider)
    if (withExpiration)
      session + (SESSION_EXPIRES_KEY, u.expires.toString)
    else
      session - SESSION_EXPIRES_KEY
  }

  /**
   * Checks if the user is logged in (also checks the expiration).
   * @param session
   * @return
   */
  def isLoggedIn(session: Session): Boolean = {
    val idAndProviderAreNotEmpty = session.get(SESSION_USER_KEY).isDefined && session.get(SESSION_PROVIDER_KEY).isDefined
    val providerIsRegistered = com.github.kompot.play2sec.authentication.providers.hasProvider(session.get(SESSION_PROVIDER_KEY).getOrElse(""))

    def validExpirationTime: Boolean = {
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
    }

    idAndProviderAreNotEmpty && providerIsRegistered && validExpirationTime
  }

  private def getExpiration(session: Session): Long = {
    session.get(SESSION_EXPIRES_KEY).map { x =>
//      Logger.info(s"expires key is $x")
      // unknown error "value toLong is not a member of String" when using
      // x.toLong
      java.lang.Long.parseLong(x)
    }.getOrElse(AuthUser.NO_EXPIRATION)

    //    if (!session.get(EXPIRES_KEY).isDefined) AuthUser.NO_EXPIRATION
//    long expires;
//    if (!session.get(EXPIRES_KEY).isEmpty()) {
//      try {
//        val expires = Long.parseLong(session.get(EXPIRES_KEY).get());
//      } catch (final NumberFormatException nfe) {
//        val expires1 = AuthUser.NO_EXPIRATION;
//      }
//    } else {
//      expires = AuthUser.NO_EXPIRATION;
//    }
//    return expires;
  }

  def logout(session: Session): Result = {
    Logger.info("Logging out and redirecting to " +
        getUrl(use[PlaySecPlugin].afterLogout, CFG_AFTER_LOGOUT_FALLBACK))
//    session.$minus(USER_KEY);
//    session.$minus(PROVIDER_KEY);
//    session.$minus(EXPIRES_KEY);

    // shouldn't be in any more, but just in case lets kill it from the
    // cookie
//    session.$minus(ORIGINAL_URL);

    Results.Redirect(
      getUrl(use[PlaySecPlugin].afterLogout, CFG_AFTER_LOGOUT_FALLBACK), REDIRECT_STATUS)
    .withNewSession
//        .withNewSession
//        .withSession(session - USER_KEY - PROVIDER_KEY - EXPIRES_KEY - ORIGINAL_URL)
  }

  /**
   * Get the user with which we are logged in - is null
   * if we are not logged in (does NOT check expiration)
   * @param session
   * @return
   */
  def getUser(session: Session): Option[AuthUser] = {
    (session.get(SESSION_PROVIDER_KEY), session.get(SESSION_USER_KEY)) match {
      case (Some(provider), Some(id)) =>
        Some(getProvider(provider).get.getSessionAuthUser(id, getExpiration(session)))
      case _ => None
    }
  }

  def getUser[A](request: Request[A]): Option[AuthUser] = getUser(request.session)
  def isAccountAutoMerge    = getConfiguration.flatMap(_.getBoolean(
    CFG_ACCOUNT_AUTO_MERGE)).getOrElse(false)
  def isAccountAutoLink     = getConfiguration.flatMap(_.getBoolean(
    CFG_ACCOUNT_AUTO_LINK)).getOrElse(false)
  def isAccountMergeEnabled = getConfiguration.flatMap(_.getBoolean(
    CFG_ACCOUNT_MERGE_ENABLED)).getOrElse(false)

  def getUrl(c: Call, settingFallback: String): String = {
    // TODO: should avoid nulls and checking for them

    // this can be null if the user did
    // not correctly define the resolver
    if (c != null) {
      c.url
    } else {
      // go to root instead, but log this
      Logger.warn("Resolver did not contain information about where to go - redirecting to /")
      getConfiguration.flatMap(_.getString(settingFallback)).getOrElse {
        Logger.error("Config setting '" + settingFallback + "' was not present!")
        "/"
      }
    }
  }

  // TODO: what is this for?
  def getProvider(providerKey: String): Option[AuthProvider] =
    // TODO direct get
    com.github.kompot.play2sec.authentication.providers.get(providerKey)
//match {
//    case a.providers.AuthProvider => _
//    case _ => throw new AuthException("Provider %s is not defined".format(providerKey))
//  }

  // TODO is it used?
  def link[A](request: Request[A], link: Boolean): Future[SimpleResult] = {
    val linkUser = getLinkUser(request.session)

    linkUser match {
      case None => {
        Logger.warn("User to be linked not found.")
        return Future.successful(Results.Forbidden("User to be linked not found."))
      }
      case Some(_) =>
    }

    removeLinkUser(request.session)
    loginAndRedirect(request, linkOrSignupUser(request, link, linkUser))
  }

  private def linkOrSignupUser[A](request: Request[A], link: Boolean,
      linkUser: Option[AuthUser]): Future[AuthUser] = {
    if (link) {
      // User accepted link - add account to existing local user
      getUserService.link(getUser(request.session), linkUser.get)
    } else {
      // User declined link - create new user
      try {
        signupUser(linkUser.get)
      } catch {
        case e: AuthException => throw e
//          return Results.InternalServerError(e.getMessage)
      }
    }
  }

  def getLinkUser(session: Session): Option[AuthUser] =
    getUserFromCache(session, LINK_USER_KEY)

  def loginAndRedirect[A](request: Request[A], loginUser: Future[AuthUser]): Future[SimpleResult] = {
    for {
      lu <- loginUser
    } yield {
      val newSession = storeUser(request, lu)
      // TODO: ajax call, is there a good way to check whether it was ajax request
      if (request.body.isInstanceOf[AnyContentAsJson]) {
        use[PlaySecPlugin].afterAuthJson(lu).withSession(newSession - SESSION_ORIGINAL_URL)
      } else {
        Results.Redirect(getJumpUrl(request), REDIRECT_STATUS).withSession(newSession - SESSION_ORIGINAL_URL)
      }
    }
  }

  def merge(request: Request[AnyContent], merge: Boolean): Future[SimpleResult] = {
    val mergeUser = getMergeUser(request.session)

    mergeUser match {
      case None => {
        Logger.warn("User to be merged not found.")
        return Future.successful(Results.Forbidden("User to be merged not found."))
      }
      case Some(_) =>
    }

    val loginUser: Future[AuthUser] = if (merge) {
      // User accepted merge, so do it
      getUserService.merge(mergeUser.get, getUser(request.session))
    } else {
      // User declined merge, so log out the old user, and log out with
      // the new one
      Future.successful(mergeUser.get)
    }
    removeMergeUser(request.session)
    loginAndRedirect(request, loginUser)
  }

  def removeMergeUser(session: Session) {
    removeFromCache(session, MERGE_USER_KEY)
  }

  def removeFromCache(session: Session, key: String): Option[Any] = {
    val o = getFromCache(session, key)
    val k = getCacheKey(session, key)
    play.api.cache.Cache.remove(k._1)
    o
  }

  private def getCacheKey(session: Session, key: String): (String, String) = {
    val id = getPlayAuthSessionId(session)
    (id + "_" + key, id)
  }

  private def getFromCache(session: Session, key: String): Option[Any] = {
    // TODO: do not use play cache, use some pluggable api that can be overriden by user
    play.api.cache.Cache.get(getCacheKey(session, key)._1)
  }

  private def getUserFromCache(session: Session, key: String): Option[AuthUser] = {
    val o = getFromCache(session, key)
    o match {
      case Some(AuthUser) => Some(o.get.asInstanceOf[AuthUser])
      case _ => None
    }
  }

  def storeMergeUser(authUser: AuthUser, session: Session) {
    // TODO the cache is not ideal for this, because it
    // might get cleared any time
    storeUserInCache(session, MERGE_USER_KEY, authUser)
  }

  def getMergeUser(session: Session): Option[AuthUser] = {
    getUserFromCache(session, MERGE_USER_KEY)
  }

  def storeLinkUser(authUser: AuthUser, session: Session) {
    // TODO the cache is not good for this
    // it might get cleared any time
    storeUserInCache(session, LINK_USER_KEY, authUser)
  }

  def removeLinkUser(session: Session) = {
    removeFromCache(session, LINK_USER_KEY)
  }

  // TODO session value set is not stored later with the response
  private def getPlayAuthSessionId(session: Session): String = {
    session.get(SESSION_ID_KEY).getOrElse(java.util.UUID.randomUUID().toString)
  }

  private def storeUserInCache(session: Session, key: String, authUser: AuthUser) = {
    storeInCache(session, key, authUser)
  }

  def storeInCache(session: Session, key: String, o: AnyRef): Session = {
    val cacheKey = getCacheKey(session, key)
    play.api.cache.Cache.set(cacheKey._1, o)
    session + (SESSION_ID_KEY, cacheKey._2)
  }

  private def getJumpUrl[A](request: Request[A]): String = {
    getOriginalUrl(request).getOrElse {
      getUrl(
        use[PlaySecPlugin].afterAuth,
        CFG_AFTER_AUTH_FALLBACK
      )
    }
  }

  @throws(scala.Predef.classOf[AuthException])
  def signupUser(u: AuthUser): Future[AuthUser] = {
    for {
      id <- getUserService.save(u)
    } yield {
      if (id == None) {
        throw new AuthException(Messages.get("playauthenticate.core.exception.singupuser_failed"))
      }
      u
    }
  }

  def handleAuthentication[A](provider: String, request: Request[A],
      payload: Option[Case.Value] = None): Future[SimpleResult] = {
     for {
       // TODO direct get
       auth <- getProvider(provider).get.authenticate(request, payload)
     } yield {
       auth match {
         case LoginSignupResult(Some(result), _, _, _) =>
           result
         case LoginSignupResult(_, Some(url), _, Some(session)) =>
           Results.Redirect(url, REDIRECT_STATUS).withSession(session)
         case LoginSignupResult(_, Some(url), _, _) =>
           Results.Redirect(url, REDIRECT_STATUS)
         case LoginSignupResult(_, _, Some(authUser), _) => {
           // TODO blocking
           import scala.concurrent.duration._
           Await.result(processUser(request, auth.authUser.get), 10.second)
         }
       }
     }
  }

  private def processUser[A](request: Request[A], newUser: AuthUser): Future[SimpleResult] = {
    Logger.info("User identity found.")
    // We might want to do merging here:
    // Adapted from:
    // http://stackoverflow.com/questions/6666267/architecture-for-merging-multiple-user-accounts-together
    // 1. The account is     linked to a local account and NO session cookie is present
    // --> Login
    // 2. The account is     linked to a local account and  a session cookie is present
    // --> Merge
    // 3. The account is NOT linked to a local account and NO session cookie is present
    // --> Signup
    // 4. The account is NOT linked to a local account and  a session cookie is present
    // --> Link

    var oldUser = getUser(request.session)
    val isLoggggedIn = isLoggedIn(request.session)
    val b = for {
      oldIdentity <- if (isLoggggedIn) getUserService.getByAuthUserIdentity(oldUser.get) else Future.successful(None)
      loginIdentity <- getUserService.getByAuthUserIdentity(newUser)
    } yield {
      // TODO: could never fall into this if :(
      if (isLoggggedIn && oldIdentity == None) {
        Logger.info("User is logged in but identity is not found. " +
            "Probably session has expired. Will log out.")
        oldUser = None
        logout(request.session)
      }

      val isLinked = loginIdentity != None

      Logger.info(s"IsLinked: $isLinked, isLoggggedIn: $isLoggggedIn")
      val loginUser: Future[AuthUser] = (isLinked, isLoggggedIn) match {
        case (true, false) => {
          Logger.info("Performing login.")
          Future.successful(newUser)
        }
        case (true, true) => {
          Logger.info("Performing merge.")
          if (isAccountMergeEnabled && !loginIdentity.equals(oldIdentity)) {
            if (isAccountAutoMerge) {
              Logger.info("Auto merge is active.")
              getUserService.merge(newUser, oldUser)
            } else {
              Logger.info("Auto merge is not active.")
              storeMergeUser(newUser, request.session)
              return Future.successful(Results.Redirect(use[PlaySecPlugin].askMerge))
            }
          } else {
            Logger.info("Doing nothing, auto merging is not enabled or " +
                "already logged in with this user.")
            // the currently logged in user and the new login belong
            // to the same local user,
            // or Account merge is disabled, so just change the log
            // in to the new user
            Future.successful(newUser)
          }
        }
        case (false, false) => {
          Logger.info("Performing sign up.")
          signupUser(newUser)
        }
        case (false, true) => {
          if (isAccountAutoLink) {
            Logger.info(s"Linking additional account.")
            getUserService.link(oldUser, newUser)
          } else {
            Logger.info(
              s"Will not link additional account. Auto linking is disabled.")
            storeLinkUser(newUser, request.session)
            return Future.successful(Results.Redirect(use[PlaySecPlugin].askLink))
          }
        }
      }
      loginAndRedirect(request, loginUser)
    }
    // TODO mega trash with control flow
    for {
      b1 <- b
      b2 <- b1
    } yield b2
  }
}
