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

/**
 * Represents a subject of a token, which can be a user, an application, or any other entity
 *
 * @author RollW
 */
interface TokenSubject {
    val id: String

    val name: String

    val type: SubjectType

    /**
     * The detail of the subject, which can be any additional information
     * such as user details, application details, etc.
     */
    val detail: Any

    /**
     * The factory interface for creating token subjects.
     */
    interface Factory {
        /**
         * Gets a token subject from the given ID and subject type.
         */
        fun fromSubject(id: String, subjectType: SubjectType): TokenSubject

        /**
         * Checks if this factory supports the given subject type.
         *
         * @param type The subject type to check.
         * @return true if this factory supports the subject type, false otherwise.
         */
        fun supports(type: SubjectType): Boolean
    }
}