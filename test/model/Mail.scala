/*
 * Copyright (c) 2013
 */

package model

case class Mail(subject: String, recipients: Array[String], body: String, from: String = "") {
  assert(!subject.isEmpty)
  assert(!recipients.isEmpty)
}
