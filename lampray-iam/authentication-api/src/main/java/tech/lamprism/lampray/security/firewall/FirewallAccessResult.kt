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
data class FirewallAccessResult @JvmOverloads constructor(
    val case: Case = Case.NEUTRAL,
    val message: String = "",
) {
    enum class Case {
        /**
         * Allow the request to pass through the firewall, and will not pass the
         * request into the next firewalls.
         */
        ALLOW,

        /**
         * Allow the request to pass through the firewall, but they still need to go
         * through the next firewalls.
         */
        NEUTRAL,

        /**
         * Deny the request and stop the request.
         */
        DENY
    }

    companion object {
        @JvmField
        val NEUTRAL = FirewallAccessResult(Case.NEUTRAL)

        @JvmField
        val ALLOW = FirewallAccessResult(Case.ALLOW)

        @JvmField
        val DENY = FirewallAccessResult(Case.DENY)
    }
}
