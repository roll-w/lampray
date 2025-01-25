/*
 * Copyright (C) 2023 RollW
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

package tech.lamprism.lampray.security.firewall.blocklist

import inet.ipaddr.IPAddressString
import tech.lamprism.lampray.security.firewall.IdentifierType
import tech.lamprism.lampray.security.firewall.RequestIdentifier
import tech.lamprism.lampray.user.UserTrait
import java.time.OffsetDateTime

/**
 * @author RollW
 */
class InMemoryBlocklist : Blocklist {
    private val ipBlocklist = hashMapOf<IPAddressString, BlocklistEntry>()
    private val userBlocklist = hashMapOf<UserTrait, BlocklistEntry>()

    override fun addAll(entries: Collection<BlocklistEntry>) {
        entries.forEach {
            plus(it)
        }
    }

    override fun plus(item: BlocklistEntry) = apply {
        when (item.type) {
            IdentifierType.IP -> {
                ipBlocklist[IPAddressString(item.identifier)] = item
            }

            IdentifierType.USER -> {
                userBlocklist[UserTrait.of(item.identifier.toLong())] = item
            }
        }
    }

    override fun minus(entry: BlocklistEntry) = apply {
        when (entry.type) {
            IdentifierType.IP -> {
                ipBlocklist.remove(IPAddressString(entry.identifier))
            }

            IdentifierType.USER -> {
                userBlocklist.remove(UserTrait.of(entry.identifier.toLong()))
            }
        }
    }

    override fun plusAssign(entry: BlocklistEntry) {
        plus(entry)
    }

    override fun minusAssign(entry: BlocklistEntry) {
        minus(entry)
    }

    override fun contains(requestIdentifier: RequestIdentifier): Boolean {
        val now = OffsetDateTime.now()
        return when (requestIdentifier.type) {
            IdentifierType.IP -> checkIp(requestIdentifier.identifier, now)
            IdentifierType.USER -> checkUser(requestIdentifier.identifier, now)
        }
    }

    private fun checkIp(
        ip: String,
        now: OffsetDateTime = OffsetDateTime.now()
    ): Boolean {
        val sourceIp = IPAddressString(ip)
        val iterator = ipBlocklist.iterator()
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
        val entry = userBlocklist[user] ?: return false
        if (entry.expiration.isBefore(now)) {
            userBlocklist.remove(user)
            return false
        }
        return true
    }

    override fun clear() {
        ipBlocklist.clear()
        userBlocklist.clear()
    }

    override fun iterator(): Iterator<BlocklistEntry> {
        removeExpired()
        return (ipBlocklist.values + userBlocklist.values)
            .sortedBy { entry ->
                entry.expiration
            }.iterator()
    }

    private fun removeExpired() {
        val now = OffsetDateTime.now()
        ipBlocklist.entries.removeIf { entry ->
            entry.value.expiration.isBefore(now)
        }
        userBlocklist.entries.removeIf { entry ->
            entry.value.expiration.isBefore(now)
        }
    }
}