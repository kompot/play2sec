/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authorization.scala

import play.api.mvc.Request
import java.util.regex.Pattern
import play.api.cache.Cache
import play.api.Play.current
import scala.annotation.tailrec
import play.api.data.Form
import com.github.kompot.play2sec.authorization.core.{PatternType,
DeadboltAnalyzer}
import com.github.kompot.play2sec.authorization.core.models.Subject

/**
 *
 * @author Steve Chaloner (steve@objectify.be)
 */

object  DeadboltViewSupport {
  /**
   * Used for restrict tags in the template.
   *
   * @param roles a list of String arrays.  Within an array, the roles are ANDed.  The arrays in the list are OR'd, so
   *              the first positive hit will allow access.
   * @param deadboltHandler application hook
   * @return true if the view can be accessed, otherwise false
   */
  def viewRestrict(roles: List[Array[String]],
                   deadboltHandler: DeadboltHandler,
                   request: Request[Any]): Boolean = {
    @tailrec
    def check(subject: Subject, current: Array[String], remaining: List[Array[String]]): Boolean = {
      if (DeadboltAnalyzer.checkRole(subject, current)) true
      else if (remaining.isEmpty) false
      else check(subject, remaining.head, remaining.tail)
    }

    deadboltHandler.getSubject(request) match {
      case Some(subject) => {
        if (roles.headOption.isDefined) check(subject, roles.head, roles.tail)
        else false
      }
      case None => false
    }
  }

  /**
   * Used for dynamic tags in the template.
   *
   * @param name the name of the resource
   * @param meta meta information on the resource
   * @return true if the view can be accessed, otherwise false
   */
  def viewDynamic(name: String,
                  meta: String,
                  deadboltHandler: DeadboltHandler,
                  request: Request[Any]): Boolean = {
    val resourceHandler = deadboltHandler.getDynamicResourceHandler(request)
    if (resourceHandler.isDefined) resourceHandler.get.isAllowed(name, Form("id" -> play.api.data.Forms.nonEmptyText), deadboltHandler, request)
    else throw new RuntimeException("A dynamic resource is specified but no dynamic resource handler is provided")
  }

  /**
   *
   * @param value
   * @param patternType
   * @param deadboltHandler
   * @param request
   * @return
   */
  def viewPattern(value: String,
                  patternType: PatternType.Value,
                  deadboltHandler: DeadboltHandler,
                  request: Request[Any]): Boolean = {
    def getPattern(patternValue: String): Pattern =
      Cache.getOrElse("Deadbolt." + patternValue)(Pattern.compile(patternValue))

    val subject = deadboltHandler.getSubject(request).get
    patternType match {
      case PatternType.EQUALITY => DeadboltAnalyzer.checkPatternEquality(subject, value)
      case PatternType.REGEX => DeadboltAnalyzer.checkRegexPattern(subject, getPattern(value))
      case PatternType.CUSTOM => {
        deadboltHandler.getDynamicResourceHandler(request) match {
            case Some(dynamicHandler) => {
              if (dynamicHandler.checkPermission(value, deadboltHandler, request)) true
              else false
            }
            case None =>
              throw new RuntimeException("A custom pattern is specified but no dynamic resource handler is provided")
          }
      }
    }
  }
}
