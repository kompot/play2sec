/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication

import scala.collection.immutable.HashMap

package object providers {
  // TODO: var -> val
  var providers: Map[String, AuthProvider] = new HashMap[String, AuthProvider]

  def register(provider: String, p: AuthProvider) {
    providers = providers.updated(provider, p)
    // TODO: check for duplicate keys

//    if (previous != null) {
//      Logger.warn("There are multiple AuthProviders registered for key '"
//          + provider + "'")
//    }
  }

  def unregister(provider: String) = {
    providers - provider
  }

  def get(provider: String): Option[AuthProvider] = providers.get(provider)

  def getProviders: Iterable[AuthProvider] = providers.values

  def hasProvider(provider: String): Boolean = providers.contains(provider)
}
