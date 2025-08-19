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
import tech.rollw.common.web.AuthErrorCode
import tech.rollw.common.web.system.AuthenticationException
import java.time.Duration
import java.util.UUID

/**
 * @author RollW
 */
class AuthorizationTokenManagerImpl(
    private val authorizationTokenProviders: List<AuthorizationTokenProvider>,
    private val revokeTokenStorage: RevokeTokenStorage
) : AuthorizationTokenManager {

    private fun findAuthorizationTokenProvider(
        tokenType: TokenType
    ): AuthorizationTokenProvider {
        return authorizationTokenProviders.firstOrNull { it.supports(tokenType) }
            ?: throw IllegalArgumentException(
                "No AuthorizationTokenProvider found for token type: $tokenType"
            )
    }

    override fun createToken(
        subject: TokenSubject,
        tokenSignKeyProvider: TokenSignKeyProvider,
        tokenType: TokenType,
        expiryDuration: Duration,
        authorizedScopes: Collection<AuthorizationScope>,
        tokenFormat: TokenFormat
    ): AuthorizationToken {
        val tokenId = UUID.randomUUID().toString()
        val authorizationToken = findAuthorizationTokenProvider(tokenType).createToken(
            subject, tokenSignKeyProvider, tokenId,
            tokenType, expiryDuration, authorizedScopes, tokenFormat
        )
        return authorizationToken
    }

    override fun parseToken(
        token: AuthorizationToken,
        tokenSignKeyProvider: TokenSignKeyProvider
    ): MetadataAuthorizationToken {
        val provider = findAuthorizationTokenProvider(token.tokenType)
        return provider.parseToken(token, tokenSignKeyProvider)
    }

    override fun exchangeToken(
        token: AuthorizationToken,
        tokenSignKeyProvider: TokenSignKeyProvider,
        newTokenType: TokenType,
        expiryDuration: Duration,
        authorizedScopes: Collection<AuthorizationScope>,
        tokenFormat: TokenFormat
    ): MetadataAuthorizationToken {
        val parseToken = findAuthorizationTokenProvider(token.tokenType)
            .parseToken(token, tokenSignKeyProvider)
        // TODO: support other token types in exchange
        if (parseToken.tokenType != TokenType.REFRESH) {
            throw AuthenticationException(AuthErrorCode.ERROR_INVALID_TOKEN, "Only refresh tokens can be exchanged.")
        }
        if (isTokenRevoked(parseToken)) {
            throw AuthenticationException(
                AuthErrorCode.ERROR_INVALID_TOKEN,
                "The token has been revoked and cannot be exchanged."
            )
        }
        val newToken = findAuthorizationTokenProvider(newTokenType).createToken(
            parseToken.subject, tokenSignKeyProvider, parseToken.tokenId,
            newTokenType, expiryDuration, authorizedScopes, tokenFormat
        )
        return newToken
    }

    override fun revokeToken(token: MetadataAuthorizationToken) {
        if (revokeTokenStorage.isTokenRevoked(token)) {
            return // Token is already revoked, no action needed
        }
        revokeTokenStorage.revokeToken(token)
    }

    override fun isTokenRevoked(token: MetadataAuthorizationToken): Boolean {
        return revokeTokenStorage.isTokenRevoked(token)
    }
}