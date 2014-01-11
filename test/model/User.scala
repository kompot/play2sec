/*
 * Copyright (c) 2013
 */

package model

import com.github.kompot.play2sec.authorization.core.models.{Permission,
Subject}
import com.github.kompot.play2sec.authorization.core.DeadboltRole

case class User(_id: String, username: Option[String], password: Option[String],
    nameLast: String = "", nameFirst: String = "",
    remoteUsers: Set[RemoteUser], isBlocked: Boolean = false,
    roles: Set[String] = Set(), permissions: Set[String] = Set())
    extends Subject {

  def emailValidated: Boolean = remoteUsers.count(ru =>
    ru.provider == RemoteUserProvider.email.toString && ru.isConfirmed) == 1

  /**
   * Get email if validated. Otherwise [[scala.None]] (if not present
   * or not confirmed).
   * @return
   */
  def email: Option[String] = remoteUsers.find(ru =>
      ru.provider == RemoteUserProvider.email.toString && ru.isConfirmed).map(_.id)

  def confirmed: Boolean = !remoteUsers.filter(_.isConfirmed).isEmpty

  def usernameOrId: String = username.getOrElse(pk)

  def getRoles: List[DeadboltRole] = roles.map(DeadboltRole).to[List]

  def getPermissions: List[Permission] = permissions.map(p => new Permission {
    def getValue = p
  }).to[List]

  def pk = _id
}
