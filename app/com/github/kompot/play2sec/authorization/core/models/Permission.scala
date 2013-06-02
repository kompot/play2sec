/*
 * Copyright (c) 2013.
 */

package com.github.kompot.play2sec.authorization.core.models

/**
 * A permission that can be held by a {@link Subject}. Checks should done
 * on the value of the permission, not using object equality on the Permission
 * itself.
 *
 * @see be.objectify.deadbolt.core.models.Subject#getPermissions()
 * @author Steve Chaloner (steve@objectify.be)
 */
trait Permission {
    /**
     * Get the value of the permission.
     *
     * @return a non-null String representation of the permission
     */
    def getValue: String
}
