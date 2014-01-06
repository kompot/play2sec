package com.github.kompot.play2sec.authentication.providers

import play.api.mvc.Request

class MyUsernamePasswordAuthProviderShortLiving(app: play.Application) extends MyUsernamePasswordAuthProvider(app) {
  override protected def buildLoginAuthUser[A](login: (String, String), request: Request[A]) =
    new MyLoginUsernamePasswordAuthUser(login._2, login._1, System.currentTimeMillis + 1)
}
