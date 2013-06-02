/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authorization.core

import com.github.kompot.play2sec.authorization.core.models.Subject
import java.util.regex.Pattern

/**
 * This carries out static (i.e. non-dynamic) checks.
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
object DeadboltAnalyzer
{
    /**
     * Checks if the subject has all the role names.
     * In other words, this gives AND support.
     *
     * @param subject the subject
     * @param roleNames the role names. Any role name starting with ! will
     *                   be negated.
     * @return true if the subject meets the restrictions (so access
     *         will be allowed), otherwise false
     */
    def checkRole(subject: Subject, roleNames: Array[String]): Boolean =
      hasAllRoles(subject, roleNames)

    /**
     * Gets the role name of each role held.
     *
     * @param subject the subject
     * @return a non-null list containing all role names
     */
    private def getRoleNames(subject: Subject): List[String] = subject.getRoles.map(_.getName)

    /**
     * Check if the subject has the given role.
     *
     * @param subject the subject
     * @param roleName the name of the role
     * @return true iff the subject has the role represented by the role name
     */
    def hasRole(subject: Subject, roleName: String): Boolean = getRoleNames(subject).contains(roleName)

    /**
     * Check if the {@link Subject} has all the roles given in the roleNames array.
     * Note that while a Subject must have all the roles, it may also have other roles.
     *
     * @param subject the subject
     * @param roleNames the names of the required roles
     * @return true iff the subject has all the roles
     */
    def hasAllRoles(subject: Subject, roleNames: Array[String]): Boolean = {
      val currentRoles = getRoleNames(subject)
      roleNames.forall { role =>
        if (!role.startsWith("!"))
          currentRoles.contains(role)
        else
          !currentRoles.contains(role.substring(1))
      }
    }

    /**
     * @param subject
     * @param pattern
     * @return
     */
    def checkRegexPattern(subject: Subject, pattern: Pattern): Boolean =
      subject.getPermissions.forall(p => pattern.matcher(p.getValue).matches())

    /**
     * @param subject
     * @param patternValue
     * @return
     */
    def checkPatternEquality(subject: Subject, patternValue: String): Boolean =
      subject.getPermissions.forall(p => p.getValue == patternValue)
}
