/*
 * Copyright (c) 2013
 */

package bootstrap

import com.softwaremill.macwire.MacwireMacros._
import model.{MailService, TokenService, UserService}

trait MacWireModule {
  lazy val tokenService = wire[TokenService]
  lazy val userService = wire[UserService]
  lazy val mailService = wire[mock.MailService]
}
