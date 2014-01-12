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
