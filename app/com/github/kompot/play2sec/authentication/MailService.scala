/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication

import akka.actor.Cancellable

trait MailService {
  def getEmailName(email: String, name: String): String
  def sendMail(subject: String, recipients: Array[String], body: String): Cancellable
}
