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

import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of RevokeTokenStorage for development and testing.
 *
 * @author RollW
 */
class InMemoryTokenRevokeStorage : RevokeTokenStorage {
    private val revokedTokens = ConcurrentHashMap<String, OffsetDateTime>()

    override fun cleanupExpiredRevocations(expireAt: OffsetDateTime) {
        // Remove revoked tokens that were revoked before the cutoff time
        val expiredTokens = revokedTokens.filterValues { it.isBefore(expireAt) }.keys
        expiredTokens.forEach { revokedTokens.remove(it) }

    }

    override fun revokeToken(token: MetadataAuthorizationToken) {
        revokedTokens[token.tokenId] = OffsetDateTime.now()
    }

    override fun isTokenRevoked(token: MetadataAuthorizationToken): Boolean {
        return revokedTokens.containsKey(token.tokenId)
    }
}
