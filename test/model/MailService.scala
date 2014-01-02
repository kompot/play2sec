/*
 * Copyright (c) 2013
 */

package model

import com.github.kompot.play2sec.authentication
import com.typesafe.plugin.MailerPlugin
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import play.api.{Logger, Configuration}
import akka.actor.Cancellable
import play.libs.Akka
import play.api.Play.current
import com.typesafe.plugin

class MailService extends authentication.MailService {
  val getConfiguration = play.api.Play.current.configuration.
                         getConfig(MailService.CONFIG_BASE).get
  val delay = Duration(getConfiguration.getLong(MailService.SettingKeys.DELAY).
                       getOrElse(1L), MILLISECONDS)

  val fromConfig = getConfiguration.getConfig(MailService.SettingKeys.FROM).get
  val email = fromConfig.getString(MailService.SettingKeys.FROM_EMAIL).get
  val name = fromConfig.getString(MailService.SettingKeys.FROM_NAME).get
  private val sender = getEmailName(email, name)

  // getConfiguration().getString(SettingKeys.VERSION, null).get();
  def getVersion = "0.1"

  private class MailJob(mail: Mail) extends Runnable {
    def run() {
      Logger.info("Sending mail to " + mail.recipients);
      // TODO initializing this plugin as a class member results in StackOverflow
      // might be due to MacWire injection

      val plugin = play.api.Play.application.plugin(classOf[MailerPlugin]).get
      val api = plugin.email
      api.setSubject(mail.subject)
      api.setRecipient(mail.recipients: _*)
      api.setFrom(mail.from)
      api.addHeader("X-Mailer", MailService.MAILER + getVersion)
      api.sendHtml(mail.body)
    }
  }

  def sendMail(subject: String, recipients: Array[String], body: String): Cancellable = {
    Akka.system.scheduler.scheduleOnce(delay, new MailJob(new Mail(subject, recipients, body, sender)))
  }

  def getEmailName(email: String, name: String): String = {
    assert(!email.trim.isEmpty)
    val sb = new StringBuilder()
    val hasName = !name.trim.isEmpty
    if (hasName) {
      sb.append("\"")
      sb.append(name)
      sb.append("\" <")
    }
    sb.append(email)
    if (hasName) {
      sb.append(">")
    }
    sb.toString()
  }
}

object MailService {
  val MAILER = "play2sec-mail/"
  val CONFIG_BASE = "play2sec-mail"

  object SettingKeys {
    val FROM = "from"
    val FROM_EMAIL = "email"
    val FROM_NAME = "name"
    val DELAY = "delay"
    val VERSION = "version"
  }
}
