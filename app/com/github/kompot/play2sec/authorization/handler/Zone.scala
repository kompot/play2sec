/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authorization.handler

import com.github.kompot.play2sec.authorization.core.models.Subject

trait Zone {
  def allowedToEverybody = false
  def allowedToSelf = true
  def allowedToAdmin = true

  def name = getClass.getSimpleName

  def allowed(maybeSubject: Option[Subject], allowed: Option[Subject] => Boolean): Boolean
}
