/*
 * Copyright (c) 2013
 */

package bootstrap

import play.api.GlobalSettings

object Global extends GlobalSettings {
  object Injector extends MacWireModule {
  }
}
