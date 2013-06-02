/*
 * Copyright (c) 2013.
 */

package com.typesafe

import play.api._

package object plugin {
  /**
   * Provides easy access to plugins.
   */
  def use[A <: Plugin](implicit app: Application, m: Manifest[A]) = {
    app.plugin[A].getOrElse(throw new RuntimeException(m.runtimeClass.toString + " plugin should be available at this point"))
  }
}
