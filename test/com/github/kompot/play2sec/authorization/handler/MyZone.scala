/*
 * Copyright (c) 2013
 */

package com.github.kompot.play2sec.authorization.handler

import com.github.kompot.play2sec.authorization.core.DeadboltRole
import com.github.kompot.play2sec.authorization.core.models.Subject

sealed trait MyZone extends Zone {
  def allowed(maybeSubject: Option[Subject], allowed: Option[Subject] => Boolean): Boolean = {
    if (allowedToEverybody) {
      true
    } else if (allowedToAdmin && maybeSubject.exists(_.getRoles.contains(DeadboltRole("admin")))) {
      true
    } else if (allowedToSelf && allowed(maybeSubject)) {
      true
    } else {
      false
    }
  }
}

case object UserEditGet extends MyZone
case object UserViewGet extends MyZone {
  override def allowedToEverybody = true
}
case object UserEditUpdate extends MyZone
