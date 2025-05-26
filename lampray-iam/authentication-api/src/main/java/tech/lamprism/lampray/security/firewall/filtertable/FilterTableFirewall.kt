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

import tech.lamprism.lampray.security.firewall.Firewall
import tech.lamprism.lampray.security.firewall.FirewallAccessRequest
import tech.lamprism.lampray.security.firewall.FirewallAccessResult
import tech.lamprism.lampray.security.firewall.FirewallAccessResult.Case
import tech.lamprism.lampray.security.firewall.IdentifierType
import tech.lamprism.lampray.security.firewall.RequestIdentifier
import tech.lamprism.lampray.user.UserIdentity

/**
 * @author RollW
 */
class FilterTableFirewall(
    private val filterTable: FilterTable
) : Firewall {

    override fun verifyRequest(request: FirewallAccessRequest): FirewallAccessResult {
        if (request.requestUser != null) {
            val result = checkUser(request.requestUser)
            if (result.case != Case.NEUTRAL) {
                return result
            }
        }
        val result = checkIp(request.requestIpAddress)
        if (result.case != Case.NEUTRAL) {
            return result
        }
        return FirewallAccessResult.NEUTRAL
    }

    private fun checkUser(userIdentity: UserIdentity): FirewallAccessResult {
        val userIdentifier = RequestIdentifier(
            userIdentity.userId.toString(),
            IdentifierType.USER
        )
        val filterEntry = filterTable[userIdentifier] ?: return FirewallAccessResult.NEUTRAL
        return when (filterEntry.mode) {
            FilterMode.ALLOW -> FirewallAccessResult(Case.ALLOW)
            FilterMode.DENY -> FirewallAccessResult(Case.DENY, "User [${userIdentity.userId}] is denied")
        }
    }

    private fun checkIp(ip: String): FirewallAccessResult {
        val ipIdentifier = RequestIdentifier(ip, IdentifierType.IP)
        val filterEntry = filterTable[ipIdentifier] ?: return FirewallAccessResult.NEUTRAL
        return when (filterEntry.mode) {
            FilterMode.ALLOW -> FirewallAccessResult(Case.ALLOW)
            FilterMode.DENY -> FirewallAccessResult(Case.DENY, "IP [$ip] is denied")
        }
    }

    override fun clearAllContext() {
        filterTable.clear()
    }
}