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

import inet.ipaddr.IPAddressString
import tech.lamprism.lampray.security.firewall.IdentifierType
import tech.lamprism.lampray.security.firewall.RequestIdentifier
import tech.lamprism.lampray.user.UserTrait
import java.time.OffsetDateTime

/**
 * @author RollW
 */
class InMemoryFilterTable : FilterTable {
    private val ipFilterlist = hashMapOf<IPAddressString, FilterEntry>()
    private val userFilterlist = hashMapOf<UserTrait, FilterEntry>()

    override fun addAll(entries: Collection<FilterEntry>) {
        entries.forEach {
            plus(it)
        }
    }

    override fun plus(entry: FilterEntry) = apply {
        when (entry.type) {
            IdentifierType.IP -> {
                val ipAddressString = IPAddressString(entry.identifier)
                if (!ipAddressString.isValid) {
                    throw IllegalArgumentException("IP address '${entry.identifier}' is invalid")
                }
                ipFilterlist[ipAddressString] = entry
            }

            IdentifierType.USER -> {
                userFilterlist[UserTrait.of(entry.identifier.toLong())] = entry
            }
        }
    }

    override fun minus(entry: FilterEntry) = apply {
        when (entry.type) {
            IdentifierType.IP -> {
                val ipAddressString = IPAddressString(entry.identifier)
                if (!ipAddressString.isValid) {
                    throw IllegalArgumentException("IP address '${entry.identifier}' is invalid")
                }
                ipFilterlist.remove(ipAddressString)
            }

            IdentifierType.USER -> {
                userFilterlist.remove(UserTrait.of(entry.identifier.toLong()))
            }
        }
    }

    override fun plusAssign(entry: FilterEntry) {
        plus(entry)
    }

    override fun minusAssign(entry: FilterEntry) {
        minus(entry)
    }

    override fun contains(requestIdentifier: RequestIdentifier): Boolean {
        val now = OffsetDateTime.now()
        return when (requestIdentifier.type) {
            IdentifierType.IP -> checkIp(requestIdentifier.identifier, now)
            IdentifierType.USER -> checkUser(requestIdentifier.identifier, now)
        }
    }

    override fun get(requestIdentifier: RequestIdentifier): FilterEntry? {
        val now = OffsetDateTime.now()
        return when (requestIdentifier.type) {
            IdentifierType.IP -> {
                getIpEntry(requestIdentifier, now)
            }
            IdentifierType.USER -> {
                val userId = requestIdentifier.identifier.toLong()
                val filterEntry = userFilterlist[UserTrait.of(userId)]
                if (filterEntry == null || filterEntry.expiration.isBefore(now)) {
                    userFilterlist.remove(UserTrait.of(userId))
                    null
                } else filterEntry
            }
        }
    }

    private fun getIpEntry(
        requestIdentifier: RequestIdentifier,
        now: OffsetDateTime
    ): FilterEntry? {
        val ipAddressString = IPAddressString(requestIdentifier.identifier)
        if (!ipAddressString.isValid) {
            throw IllegalArgumentException("IP address '${requestIdentifier.identifier}' is invalid")
        }
        val filterEntry = ipFilterlist[ipAddressString]
        if (filterEntry != null) {
            if (filterEntry.expiration.isBefore(now)) {
                ipFilterlist.remove(ipAddressString)
                return null
            }
            return filterEntry
        }

        return ipFilterlist.keys.asSequence().filter {
            it.contains(ipAddressString)
        }.mapNotNull {
            ipFilterlist[it]
        }.filter {
            it.expiration.isAfter(now)
        }.sortedBy {
            it.expiration
        }.firstOrNull()
    }

    private fun checkIp(
        ip: String,
        now: OffsetDateTime = OffsetDateTime.now()
    ): Boolean {
        val sourceIp = IPAddressString(ip)
        if (!sourceIp.isValid) {
            throw IllegalArgumentException("IP address '$ip' is invalid")
        }
        val iterator = ipFilterlist.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.expiration.isBefore(now)) {
                iterator.remove()
                continue
            }
            if (entry.key.contains(sourceIp)) {
                return true
            }
        }
        return false
    }

    private fun checkUser(
        userId: String,
        now: OffsetDateTime = OffsetDateTime.now()
    ): Boolean {
        val user = UserTrait.of(userId.toLong())
        val entry = userFilterlist[user] ?: return false
        if (entry.expiration.isBefore(now)) {
            userFilterlist.remove(user)
            return false
        }
        return true
    }

    override fun clear() {
        ipFilterlist.clear()
        userFilterlist.clear()
    }

    override fun iterator(): Iterator<FilterEntry> {
        removeExpired()
        return (ipFilterlist.values + userFilterlist.values)
            .sortedBy { entry ->
                entry.expiration
            }.iterator()
    }

    private fun removeExpired() {
        val now = OffsetDateTime.now()
        ipFilterlist.entries.removeIf { entry ->
            entry.value.expiration.isBefore(now)
        }
        userFilterlist.entries.removeIf { entry ->
            entry.value.expiration.isBefore(now)
        }
    }
}