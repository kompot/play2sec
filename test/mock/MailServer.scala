package mock

import java.lang.String
import model.Mail
import scala.collection.parallel.mutable

object MailServer {
  /**
   * [[mock.MailBox]] by email address.
   */
  val boxes = mutable.ParMap[String, MailBox]()

  def sendMail(mail: Mail) {
    for (recipient <- mail.recipients) {
      val boxToDeliverTo = boxes.get(recipient).getOrElse {
        val newMailBox = new MailBox(recipient)
        boxes + (recipient, newMailBox)
        newMailBox
      }
      boxToDeliverTo.sendMessage(mail)
    }
  }
}
