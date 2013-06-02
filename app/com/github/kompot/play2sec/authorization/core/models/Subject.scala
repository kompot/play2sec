/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authorization.core.models

import scala.Option

/**
 * A Subject represents an authorisable entity, typically a user, that will
 * try to access the application.
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
trait Subject
{
    /**
     * Get all {@link Role}s held by this subject.  Ordering is not important.
     *
     * @return a non-null list of roles
     */
    def getRoles: List[Role]

    /**
     * Get all {@link Permission}s held by this subject.  Ordering is not important.
     *
     * @return a non-null list of permissions
     */
    def getPermissions: List[Permission]

    /**
     * Gets a unique identifier for the subject, such as a user name.  This is never used by Deadbolt itself,
     * and is present to provide an easy way of getting a useful piece of user information in, for example,
     * dynamic checks without the need to cast the Subject.
     *
     * @return an identifier, such as a user name or UUID.  May be null.
     */
    def usernameOrId: String

    def pk: String

    def username: Option[String]

    def confirmed: Boolean
}
