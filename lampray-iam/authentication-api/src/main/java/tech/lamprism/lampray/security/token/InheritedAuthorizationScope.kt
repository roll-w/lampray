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
package tech.lamprism.lampray.security.token

import tech.lamprism.lampray.security.authorization.AuthorizationScope

/**
 * A specialized authorization scope that indicates the token inherits scopes
 * from a specific subject, identified by its ID and type.
 *
 * Not an actual scope, but a marker for inherited scopes.
 *
 * @author RollW
 */
data class InheritedAuthorizationScope(
    val id: String,
    val type: SubjectType
) : AuthorizationScope {
    override val scope: String
        get() = "${PREFIX}${type}:$id"


    companion object {

        const val PREFIX = "inherited:"

        /**
         * Creates an instance of [InheritedAuthorizationScope] for the given subject.
         *
         * @param subject The [TokenSubject] whose scopes are to be inherited.
         * @return An instance of [InheritedAuthorizationScope].
         */
        @JvmStatic
        fun fromSubject(subject: TokenSubject): InheritedAuthorizationScope {
            return InheritedAuthorizationScope(subject.id, subject.type)
        }

        /**
         * Parses a scope string to create an [InheritedAuthorizationScope] if it matches the expected format.
         *
         * @param scope The scope string to parse.
         * @return An [InheritedAuthorizationScope] if the scope starts with "inherited:", otherwise null.
         */
        @JvmStatic
        fun parse(scope: String): InheritedAuthorizationScope? {
            return if (scope.startsWith(PREFIX)) {
                val subject = scope.removePrefix(PREFIX)
                val index = subject.indexOf(':')
                if (index <= 0 || index >= subject.length - 1) {
                    return null
                }
                val type = subject.substring(0, index)
                val id = subject.substring(index + 1)
                val subjectType = SubjectType.fromValue(type)
                InheritedAuthorizationScope(id, subjectType)
            } else {
                null
            }
        }
    }


}
