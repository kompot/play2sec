/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authentication.user

import java.util.Locale
import org.apache.commons.lang3.LocaleUtils

trait AuthUser extends AuthUserIdentity {
  def expires = AuthUser.NO_EXPIRATION

  override def toString = s"$getId@$getProvider"

  override def hashCode(): Int = {
    val prime = 31
    val result: Int = prime + (if (getId == null) 0 else getId.hashCode)
    prime * result + (if (getProvider == null) 0 else getProvider.hashCode)
  }

  override def equals(obj: Any): Boolean = {
    if (this == obj)
      return true
    if (obj == null)
      return false
    if (getClass != obj.getClass)
      return false
    val other = obj.asInstanceOf[AuthUserIdentity]
    if (getId == null) {
      if (other.getId != null)
        return false
    } else if (!getId.equals(other.getId))
      return false
    if (getProvider == null) {
      if (other.getProvider != null)
        return false
    } else if (!getProvider.equals(other.getProvider))
      return false
    true
  }
}

object AuthUser {
  val NO_EXPIRATION = -1L

  def getLocaleFromString(locale: String): Option[Locale] = {
    if (!locale.isEmpty) {
      try {
        Some(LocaleUtils.toLocale(locale))
      } catch { case e: java.lang.IllegalArgumentException =>
        try {
          Some(LocaleUtils.toLocale(locale.replace('-', '_')))
        } catch { case e1: java.lang.IllegalArgumentException =>
          None
        }
      }
    } else {
      None
    }
  }

  // TODO uncomment?
//  // T extends AuthUserIdentity & NameIdentity
//  def toString[T](identity: T): String = {
//    val sb = new StringBuilder
//    if (identity.getName() != null) {
//      sb.append(identity.getName())
//      sb.append(" ")
//    }
//    if(identity instanceof EmailIdentity) {
//      final EmailIdentity i2 = (EmailIdentity) identity
//      if (i2.getEmail() != null) {
//        sb.append("(")
//        sb.append(i2.getEmail())
//        sb.append(") ")
//      }
//    }
//    if (sb.length() == 0) {
//      sb.append(identity.getId())
//    }
//    sb.append(" @ ")
//    sb.append(identity.getProvider())
//
//    sb.toString
//  }
}
