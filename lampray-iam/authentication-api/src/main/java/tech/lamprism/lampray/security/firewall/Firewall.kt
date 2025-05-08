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
 * Firewall is used to verify the request, and throw an exception if the request is
 * not allowed.
 *
 * The firewall should not be used for authentication, but rather to filter requests
 * and reject suspicious ones, thereby mitigating the risk of system attacks.
 *
 * **Implementation notes:**
 *
 * - Should be thread-safe.
 * - Each firewall should identify each request and hold its own context on each
 *   request.
 *
 * @author RollW
 */
@JvmDefaultWithoutCompatibility
interface Firewall {
    @Throws(FirewallException::class)
    fun verifyRequest(request: FirewallAccessRequest): FirewallAccessResult

    /**
     * Clear the context for the request. Any already set restricted rules will
     * be reset and re-evaluated on the next request.
     *
     * Note: This [FirewallAccessRequest] could be partial containing only the
     * information that is required to clear the context, like the user id.
     */
    fun clearContext(request: FirewallAccessRequest) {
        // Do nothing by default
    }

    fun clearAllContext() {
        // Do nothing by default
    }

    /**
     * The priority of the firewall, the higher the value, the higher the priority.
     * The firewall with the highest priority will be executed first.
     *
     * Priority is important for the firewall request verifying, if the request is
     * denied by a firewall with a higher priority, the request will not be passed to
     * the next firewalls.
     *
     * @return the priority of the firewall, could be negative.
     */
    val priority: Int
        get() = 0
}