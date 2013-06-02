/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authorization.scala

import play.api.mvc.Request
import play.api.data.Form

/**
 *
 * @author Steve Chaloner (steve@objectify.be)
 */

trait DynamicResourceHandler
{
  /**
   * Check the access of the named resource.
   *
   * @param name the resource name
   * @param form form to get additional information on the resource from
   * @param deadboltHandler the current { @link DeadboltHandler}
   * @param request the current request
   * @return true if access to the resource is allowed, otherwise false
   */
  def isAllowed[A, B](name: String,
                   form: Form[B],
                   deadboltHandler: DeadboltHandler,
                   request: Request[A]): Boolean = false

  /**
   * Invoked when a {@link DeadboltPattern} with a {@link PatternType#CUSTOM} type is
   * used.
   *
   * @param permissionValue the permission value
   * @param deadboltHandler the current { @link DeadboltHandler}
   * @param request the current request
   * @return true if access based on the permission is  allowed, otherwise false
   */
  def checkPermission[A](permissionValue: String,
                         deadboltHandler: DeadboltHandler,
                         request: Request[A]): Boolean = false
}
