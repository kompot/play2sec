/*
 * Copyright 2012-2013 Joscha Feth, Steve Chaloner, Anton Fedchenko
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

import play.api.mvc.{Action, Results, BodyParsers}
import play.api.cache.Cache
import java.util.regex.Pattern
import play.api.Play.current
import play.api.data.{Forms, Form}
import com.github.kompot.play2sec.authorization.core.DeadboltAnalyzer
import com.github.kompot.play2sec.authorization.core.PatternType
import com.github.kompot.play2sec.authorization.handler.Zone
import com.github.kompot.play2sec.authorization.core.models.Subject


/**
 * Controller-level authorisations for Scala controllers.
 *
 * @author Steve Chaloner
 */
trait DeadboltActions extends Results with BodyParsers {

  /**
   * Restrict access to an action to users that have all the specified roles.
   *
   * @param roleNames
   * @param deadboltHandler
   * @param action
   * @tparam A
   * @return
   */
  def Restrictions[A](roleNames: Array[String],
                      deadboltHandler: DeadboltHandler)(action: Action[A]): Action[A] = {
    Restrictions[A](List(roleNames),
                    deadboltHandler)(action)
  }

  /**
   * Restrict access to an action to users that have all the specified roles
   * within a given group.  Each group, which is an array of strings,
   * is checked in turn.
   *
   * @param roleGroups
   * @param deadboltHandler
   * @param action
   * @tparam A
   * @return
   */
  def Restrictions[A](roleGroups: List[Array[String]],
                      deadboltHandler: DeadboltHandler)
                     (action: Action[A]): Action[A] = {
    Action.async(action.parser) { implicit request =>

      def check(subject: Subject, current: Array[String], remaining: List[Array[String]]): Boolean = {
        if (DeadboltAnalyzer.checkRole(subject, current)) true
        else if (remaining.isEmpty) false
        else check(subject, remaining.head, remaining.tail)
      }

      deadboltHandler.beforeAuthCheck(request) match {
          case Some(result) => result
          case _ => {
            if (roleGroups.isEmpty) deadboltHandler.onAuthFailure(request)
            else {
              deadboltHandler.getSubject(request) match {
                case Some(subject) => {
                  if (check(subject, roleGroups.head, roleGroups.tail)) action(request)
                  else deadboltHandler.onAuthFailure(request)
                }
                case _ => deadboltHandler.onAuthFailure(request)
              }
            }
          }
        }
    }
  }

  /**
   *
   * @param name
   * @param meta
   * @param deadboltHandler
   * @param action
   * @tparam A
   * @return
   */
//  def Dynamic[A](name: String,
//                 meta: String,
//                 deadboltHandler: DeadboltHandler)
//                (action: Action[A]): Action[A] = {
//    Action(action.parser) { implicit request =>
//      deadboltHandler.beforeAuthCheck(request) match {
//          case Some(result) => result
//          case _ => {
//            deadboltHandler.getDynamicResourceHandler(request) match {
//              case Some(dynamicHandler) => {
//                if (dynamicHandler.isAllowed(name, Form("id" -> Forms.nonEmptyText).bind(Map("id", meta)), deadboltHandler, request)) action(request)
//                else deadboltHandler.onAuthFailure(request)
//              }
//              case None =>
//                throw new RuntimeException("A dynamic resource is specified but no dynamic resource handler is provided")
//            }
//          }
//        }
//    }

//    Dynamic(name, form, deadboltHandler)(action)
//  }

  def Dynamic[A](zone: Zone, meta: String = "", deadboltHandler: DeadboltHandler)(action: Action[A]): Action[A] = {
    val form = Form("id" -> Forms.nonEmptyText).bind(Map(("id", meta)))
    Dynamic(zone, form, deadboltHandler)(action)
  }

  def Dynamic[A, B](zone: Zone, form: Form[B], deadboltHandler: DeadboltHandler)(action: Action[A]): Action[A] = {
    Action.async(action.parser) { implicit request =>
      deadboltHandler.beforeAuthCheck(request) match {
        case Some(result) => result
        case _ => {
          deadboltHandler.getDynamicResourceHandler(request) match {
            case Some(dynamicHandler) => {
              if (dynamicHandler.isAllowed(zone.getClass.getSimpleName, form, deadboltHandler, request))
                action(request)
              else
                deadboltHandler.onAuthFailure(request)
            }
            case None =>
              throw new RuntimeException("A dynamic resource is specified but no dynamic resource handler is provided")
          }
        }
      }
    }
  }

  /**
   *
   * @param value
   * @param patternType
   * @param deadboltHandler
   * @param action
   * @tparam A
   * @return
   */
  def Pattern[A](value: String, patternType: PatternType.Value, deadboltHandler: DeadboltHandler)
      (action: Action[A]): Action[A] = {

    def getPattern(patternValue: String): Pattern =
      Cache.getOrElse("Deadbolt." + patternValue)(java.util.regex.Pattern.compile(patternValue))

    Action.async(action.parser) {
      implicit request =>
        deadboltHandler.beforeAuthCheck(request) match {
          case Some(result) => result
          case _ => {
            // TODO direct get, might fail, convert to map
            val subject = deadboltHandler.getSubject(request).get
            patternType match {
              case PatternType.EQUALITY => {
                if (DeadboltAnalyzer.checkPatternEquality(subject, value)) action(request)
                else deadboltHandler.onAuthFailure(request)
              }
              case PatternType.REGEX => {
                if (DeadboltAnalyzer.checkRegexPattern(subject, getPattern(value))) action(request)
                else deadboltHandler.onAuthFailure(request)
              }
              case PatternType.CUSTOM => {
                deadboltHandler.getDynamicResourceHandler(request) match {
                  case Some(dynamicHandler) => {
                    if (dynamicHandler.checkPermission(value, deadboltHandler, request)) action(request)
                    else deadboltHandler.onAuthFailure(request)
                  }
                  case None =>
                    throw new RuntimeException("A custom pattern is specified but no dynamic resource handler is provided")
                }
              }
            }
          }
        }
    }
  }

  /**
   * Denies access to the action if there is no subject present.
   *
   * @param deadboltHandler
   * @param action
   * @tparam A
   * @return
   */
  def SubjectPresent[A](deadboltHandler: DeadboltHandler)(action: Action[A]): Action[A] = {
    Action.async(action.parser) { implicit request =>
      deadboltHandler.beforeAuthCheck(request) match {
            case Some(result) => result
            case _ => {
              deadboltHandler.getSubject(request) match {
                case Some(handler) => action(request)
                case None => deadboltHandler.onAuthFailure(request)
              }
            }
          }
    }
  }

  /**
   * Denies access to the action if there is a subject present.
   *
   * @param deadboltHandler
   * @param action
   * @tparam A
   * @return
   */
  def SubjectNotPresent[A](deadboltHandler: DeadboltHandler)(action: Action[A]): Action[A] = {
    Action.async(action.parser) { implicit request =>
      deadboltHandler.beforeAuthCheck(request) match {
            case Some(result) => result
            case _ => {
              deadboltHandler.getSubject(request) match {
                case Some(subject) => deadboltHandler.onAuthFailure(request)
                case None => action(request)
              }
            }
          }
    }
  }
}
