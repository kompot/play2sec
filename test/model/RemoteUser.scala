/*
 * Copyright (c) 2013
 */

package model

import java.util.Locale

case class RemoteUser(provider: String, id: String, isConfirmed: Boolean = false,
    profileLink: Option[String] = None, preferredLocale: Option[Locale] = None)
