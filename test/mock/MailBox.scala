package mock

import scala.Predef.String
import model.Mail

class MailBox(email: String) {
  var messages = List[Mail]()

  def findByContent(s: String): List[Mail] = {
    messages.filter { message =>
      message.subject.indexOf(s) != -1 ||
          message.body.indexOf(s) != -1
    }
  }

  def sendMessage(message: Mail) {
    messages = messages.::(message)
  }
}
