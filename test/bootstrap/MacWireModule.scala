/*
 * Copyright (c) 2013
 */

package bootstrap

import com.softwaremill.macwire.MacwireMacros._
import mock.{TokenStore, UserStore}

trait MacWireModule {
  lazy val tokenStore = wire[TokenStore]
  lazy val userStore = wire[UserStore]
  lazy val mailService = wire[mock.MailService]
}
