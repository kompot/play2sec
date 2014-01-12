/*
 * Copyright 2012-2014 Joscha Feth, Steve Chaloner, Anton Fedchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
