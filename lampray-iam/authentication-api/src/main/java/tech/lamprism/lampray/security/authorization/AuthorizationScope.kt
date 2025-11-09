/*
 * Copyright (C) 2023-2025 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.lamprism.lampray.security.authorization

/**
 * Represents an authorization scope.
 *
 * An authorization scope is a string that represents a resource
 * or a set of resources that a user can access or a permission
 * that a user can have.
 *
 * @author RollW
 */
@JvmDefaultWithoutCompatibility
interface AuthorizationScope {
    /**
     * The scope value of this authorization scope.
     */
    val scope: String

    val parents: Collection<AuthorizationScope>
        get() = emptyList()

    infix fun equals(other: AuthorizationScope): Boolean {
        return scope == other.scope
    }

    companion object {
        fun AuthorizationScope.hasScope(scope: AuthorizationScope): Boolean {
            if (this equals scope) {
                return true
            }
            return parents.any { it.hasScope(scope) }
        }
    }
}