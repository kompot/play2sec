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

package com.github.kompot.play2sec.authentication

import scala.collection.immutable.HashMap

package object providers {
  // TODO: var -> val
  private var providers: Map[String, AuthProvider] = new HashMap[String, AuthProvider]

  def register(provider: String, p: AuthProvider) {
    providers = providers.updated(provider, p)
    // TODO: check for duplicate keys

//    if (previous != null) {
//      Logger.warn("There are multiple AuthProviders registered for key '"
//          + provider + "'")
//    }
  }

  def unregister(provider: String) = providers - provider

  def get(provider: String): Option[AuthProvider] = providers.get(provider)

  def hasProvider(provider: Option[String]): Boolean = provider match {
    case Some(p) => providers.contains(p)
    case _       => false
  }
}
