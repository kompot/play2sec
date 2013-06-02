/*
 * Copyright (c) 2013.
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
import com.github.kompot.play2sec.authentication.providers.password.Case
import com.github.kompot.play2sec.authentication.providers.AuthProvider
import java.util.Date

package object authentication {
  val SETTING_KEY_PLAY_AUTHENTICATE = "play2sec"
  val SETTING_KEY_AFTER_AUTH_FALLBACK = "afterAuthFallback"
  val SETTING_KEY_AFTER_LOGOUT_FALLBACK = "afterLogoutFallback"
  val SETTING_KEY_ACCOUNT_MERGE_ENABLED = "accountMergeEnabled"
  val SETTING_KEY_ACCOUNT_AUTO_LINK = "accountAutoLink"
  val SETTING_KEY_ACCOUNT_AUTO_MERGE = "accountAutoMerge"

  val ORIGINAL_URL = "a-return-url"
  val USER_KEY = "a-user-id"
  val PROVIDER_KEY = "a-provider-id"
  val EXPIRES_KEY = "a-exp"
  val SESSION_ID_KEY = "a-session-id"

  // TODO null
  val MERGE_USER_KEY: String = null
  // TODO null
  val LINK_USER_KEY: String = null

  def getConfiguration: Option[Configuration] = application.configuration.getConfig(SETTING_KEY_PLAY_AUTHENTICATE)

  // TODO: remove ORIGINAL_URL from session
  def getOriginalUrl[A](request: Request[A]): Option[String] = request.session.get(ORIGINAL_URL)

  def getUserService: UserService = use[PlaySecPlugin].userService

  def storeUser[A](request: Request[A], authUser: AuthUser): Session = {
    // User logged in once more - wanna make some updates?
    val u: AuthUser = getUserService.whenLogin(authUser, request)

    val withExpiration = u.expires != AuthUser.NO_EXPIRATION

    // TODO: better way to DRY?
    Logger.debug("Will be storing user " + u)
    if (withExpiration)
      request.session +
        (USER_KEY, u.getId) +
        (PROVIDER_KEY, u.getProvider) +
        (EXPIRES_KEY, u.expires.toString)
    else
      request.session +
        (USER_KEY, u.getId) +
        (PROVIDER_KEY, u.getProvider) -
        (EXPIRES_KEY)
  }

  /**
   * Checks if the user is logged in (also checks the expiration).
   * @param session
   * @return
   */
  def isLoggedIn(session: Session): Boolean = {
    val idAndProviderAreNotEmpty = session.get(USER_KEY).isDefined && session.get(PROVIDER_KEY).isDefined
    val providerIsRegistered = com.github.kompot.play2sec.authentication.providers.hasProvider(session.get(PROVIDER_KEY).getOrElse(""))

    def validExpirationTime: Boolean = {
      if (session.get(EXPIRES_KEY).isDefined) {
        // expiration is set
        val expires = getExpiration(session)
        if (expires != AuthUser.NO_EXPIRATION) {
          // and the session expires after now
          return new Date().getTime < expires
        } else {
          true
        }
      }
      true
    }

    idAndProviderAreNotEmpty && providerIsRegistered && validExpirationTime
  }

  private def getExpiration(session: Session): Long = {
    session.get(EXPIRES_KEY).map { x =>
//      Logger.debug(s"expires key is $x")
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
    Logger.debug("Logging out and redirecting to " + getUrl(use[PlaySecPlugin].afterLogout, SETTING_KEY_AFTER_LOGOUT_FALLBACK))
//    session.$minus(USER_KEY);
//    session.$minus(PROVIDER_KEY);
//    session.$minus(EXPIRES_KEY);

    // shouldn't be in any more, but just in case lets kill it from the
    // cookie
//    session.$minus(ORIGINAL_URL);

    Results.Redirect(getUrl(use[PlaySecPlugin].afterLogout, SETTING_KEY_AFTER_LOGOUT_FALLBACK), 303).withNewSession
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
    (session.get(PROVIDER_KEY), session.get(USER_KEY)) match {
      case (Some(provider), Some(id)) =>
        Some(getProvider(provider).get.getSessionAuthUser(id, getExpiration(session)))
      case _ => None
    }
  }

  def getUser[A](request: Request[A]): Option[AuthUser] = getUser(request.session)
  def isAccountAutoMerge = getConfiguration.flatMap(_.getBoolean(SETTING_KEY_ACCOUNT_AUTO_MERGE)).getOrElse(false)
  def isAccountAutoLink = getConfiguration.flatMap(_.getBoolean(SETTING_KEY_ACCOUNT_AUTO_LINK)).getOrElse(false)
  def isAccountMergeEnabled = getConfiguration.flatMap(_.getBoolean(SETTING_KEY_ACCOUNT_MERGE_ENABLED)).getOrElse(false)

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
  def getProvider(providerKey: String): Option[AuthProvider] = com.github.kompot.play2sec.authentication.providers.get(providerKey)
//match {
//    case a.providers.AuthProvider => _
//    case _ => throw new AuthException("Provider %s is not defined".format(providerKey))
//  }

  def link(request: Request[AnyContent], link: Boolean): Result = {
    val linkUser = getLinkUser(request.session)

    linkUser match {
      case None => {
        Logger.warn("User to be linked not found.")
        return Results.Forbidden("User to be linked not found.")
      }
      case Some(_) =>
    }

    val loginUser: AuthUser =
      if (link) {
        // User accepted link - add account to existing local user
        getUserService.link(getUser(request.session), linkUser.get)
      } else {
        // User declined link - create new user
        try {
          signupUser(linkUser.get)
        } catch {
          case e: AuthException => return Results.InternalServerError(e.getMessage)
        }
      }
    removeLinkUser(request.session)
    loginAndRedirect(request, loginUser)
  }

  def getLinkUser(session: Session): Option[AuthUser] = {
    getUserFromCache(session, LINK_USER_KEY)
  }

  def loginAndRedirect[A](request: Request[A], loginUser: AuthUser): Result = {
    val newSession = storeUser(request, loginUser)
    // TODO: ajax call, is there a good way to check whether it was ajax request
    if (request.body.isInstanceOf[AnyContentAsJson]) {
      use[PlaySecPlugin].afterAuthJson(loginUser).withSession(newSession - ORIGINAL_URL)
    } else {
      Results.Redirect(getJumpUrl(request), 303).withSession(newSession - ORIGINAL_URL)
    }
  }

  def merge(request: Request[AnyContent], merge: Boolean): Result = {
    val mergeUser = getMergeUser(request.session)

    mergeUser match {
      case None => {
        Logger.warn("User to be merged not found.")
        return Results.Forbidden("User to be merged not found.")
      }
      case Some(_) =>
    }

    val loginUser: AuthUser = if (merge) {
      // User accepted merge, so do it
      getUserService.merge(mergeUser.get, getUser(request.session))
    } else {
      // User declined merge, so log out the old user, and log out with
      // the new one
      mergeUser.get
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
    // TODO the cache is not ideal for this, because
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

  private def storeUserInCache(session: Session, key: String, authUser: AuthUser) {
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
        SETTING_KEY_AFTER_AUTH_FALLBACK
      )
    }
  }

  @throws(scala.Predef.classOf[AuthException])
  def signupUser(u: AuthUser): AuthUser = {
    val id = getUserService.save(u)
    if (id == None) {
      throw new AuthException(Messages.get("playauthenticate.core.exception.singupuser_failed"))
    }
    u
  }

  def handleAuthentication[A](provider: String, request: Request[A], payload: Option[Case.Value] = None): Result = {
    getProvider(provider).map(a => a.authenticate(request, payload)).get match {
      case r: Result => r
      case url: String => {
        Results.Redirect(url, 303)
      }
      case (url: String, session: Session) => {
        Results.Redirect(url, 303).withSession(session)
      }
      case None => {
        Results.NotFound(Messages.get("playauthenticate.core.exception.provider_not_found", provider))
      }
      case newUser: AuthUser => {
        Logger.warn("33333")
        // We might want to do merging here:
        // Adapted from:
        // http://stackoverflow.com/questions/6666267/architecture-for
        // -merging-multiple-user-accounts-together
        // 1. The account is linked to a local account and no session
        // cookie is present --> Login
        // 2. The account is linked to a local account and a session
        // cookie is present --> Merge
        // 3. The account is not linked to a local account and no
        // session cookie is present --> Signup
        // 4. The account is not linked to a local account and a session
        // cookie is present --> Linking Additional account


        // TODO remove var
        var oldUser = getUser(request.session)
        val isLoggggedIn: Boolean = isLoggedIn(request.session)
        val oldIdentity = if (isLoggggedIn) getUserService.getByAuthUserIdentitySync(oldUser.get) else None
        // TODO: could never fall into this if :(
        if (isLoggggedIn && oldIdentity == None) {
          Logger.debug("User is logged in but identity is not found. " +
              "Probably session has expired. Will log out.")
          oldUser = None
          logout(request.session)
        }
        val loginIdentity = getUserService.getByAuthUserIdentitySync(newUser)
        val isLinked = loginIdentity != None

        Logger.debug(s"isLinked: $isLinked, isLoggggedIn: $isLoggggedIn")
        val loginUser: AuthUser = (isLinked, isLoggggedIn) match {
          case (true, false) => { // login
            Logger.debug("doing login")
            newUser
          }
          case (true, true) => { // merge
            Logger.debug("doing merge")
            if (isAccountMergeEnabled && !loginIdentity.equals(oldIdentity)) {
              if (isAccountAutoMerge) {
                Logger.debug("1")
                getUserService.merge(newUser, oldUser)
              } else {
                Logger.debug("2")
                storeMergeUser(newUser, request.session)
                return Results.Redirect(use[PlaySecPlugin].askMerge)
              }
            } else {
              Logger.debug("3")
              // the currently logged in user and the new login belong
              // to the same local user,
              // or Account merge is disabled, so just change the log
              // in to the new user
              newUser
            }
          }
          case (false, false) => { // signup
            Logger.debug("doing signup")
            signupUser(newUser)
          }
          case (false, true) => { // link additional account
            Logger.debug(s"doing link isAccountAutoLink $isAccountAutoLink")
            if (isAccountAutoLink) {
              getUserService.link(oldUser, newUser)
            } else {
              storeLinkUser(newUser, request.session)
              return Results.Redirect(use[PlaySecPlugin].askLink)
            }
          }
        }
        loginAndRedirect(request, loginUser)
      }
      case _ => {
        Results.InternalServerError(Messages.get("playauthenticate.core.exception.general"))
      }
    }
  }
}
