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

package tech.lamprism.lampray.security.firewall.filtertable

/**
 * @author RollW
 */
enum class FilterMode {
    /**
     * Allow the request if it matches any of the filters.
     */
    ALLOW,

    /**
     * Deny the request if it matches any of the filters.
     */
    DENY, ;

    companion object {
        @JvmStatic
        fun fromString(mode: String): FilterMode {
            return values().firstOrNull {
                it.name.equals(mode, true)
            } ?: throw IllegalArgumentException("Unknown filter mode: $mode")
        }
    }
}