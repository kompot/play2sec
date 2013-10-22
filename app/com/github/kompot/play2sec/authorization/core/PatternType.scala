/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authorization.core

/**
 * @author Steve Chaloner (steve@objectify.be)
 */
object PatternType extends Enumeration {
  /**
   * Checks the pattern against the permissions of the user. Exact,
   * case-sensitive matches only!
   */
  val EQUALITY = Value

  /**
   * A standard regular expression that will be evaluated against the
   * permissions of the Subject
   */
  val REGEX = Value

  /**
   * Perform some custom matching on the pattern.
   */
  val CUSTOM = Value
}
