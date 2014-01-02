package mock

import scala.Predef.String
import model.Mail
import akka.actor.Cancellable
import com.github.kompot.play2sec.authentication

class MailService extends authentication.MailService {
  override def sendMail(subject: String, recipients: Array[String],
      body: String): Cancellable = {
    MailServer.sendMail(new Mail(subject, recipients, body, "play2sec@kompot.name"))
    null
  }

  def getEmailName(email: String, name: String) = email
}
