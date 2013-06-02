/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authorization.core

import scala.language.implicitConversions
import com.github.kompot.play2sec.authorization.core.models.Role

case class DeadboltRole(name: String) extends Role {
  def getName = name
}
