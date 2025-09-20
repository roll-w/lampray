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

package tech.lamprism.lampray.security.firewall


/**
 * @author RollW
 */
enum class IdentifierType {
    /**
     * For IP address, it needs to be a valid IPv4 or IPv6 address
     * or a valid CIDR notation.
     *
     * For example:
     * - `12.1.1.1`
     * - `12.1.1.0/24`
     * - `2001:db8::/32`
     */
    IP,

    /**
     * For user identifier, it needs to be a valid user ID.
     *
     * Do not use the username or email as the identifier,
     * since they can be changed by the user.
     */
    USER;

    companion object {
        @JvmStatic
        fun fromString(value: String): IdentifierType {
            entries.forEach {
                if (it.name.equals(value, ignoreCase = true)) {
                    return it
                }
            }
            throw IllegalArgumentException("No identifier value=$value")
        }
    }
}