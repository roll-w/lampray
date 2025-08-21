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
import tech.rollw.common.web.system.AuthenticationException
import java.time.Duration

/**
 * Enhanced authorization token manager that supports different token types
 * and clear separation between transport format and functional type.
 *
 * @author RollW
 */
interface AuthorizationTokenManager {
    /**
     * Create a token with specified type and format.
     *
     * @param subject The subject of the token, which can be a user or a service.
     * @param tokenSignKeyProvider The signature provider to sign the token.
     * @param tokenType The type of the token (e.g., ACCESS, REFRESH).
     * @param expiryDuration The expiry duration of the token from now.
     * @param authorizedScopes The authorized scopes of the token. If empty, no scopes are authorized.
     * @param tokenFormat The format of the token (Bearer, Basic, etc.).
     * @return The created token with metadata.
     */
    fun createToken(
        subject: TokenSubject,
        tokenSignKeyProvider: TokenSignKeyProvider,
        tokenType: TokenType = TokenType.ACCESS,
        expiryDuration: Duration = Duration.ofHours(1),
        authorizedScopes: Collection<AuthorizationScope> = emptyList(),
        tokenFormat: TokenFormat = TokenFormat.BEARER
    ): AuthorizationToken

    /**
     * Parse the token to get the user identity and metadata.
     *
     * @param token The token.
     * @param tokenSignKeyProvider The signature provider to verify the token.
     * @return The parsed token with metadata.
     * @throws AuthenticationException If the token is invalid or expired.
     */
    @Throws(AuthenticationException::class)
    fun parseToken(
        token: AuthorizationToken,
        tokenSignKeyProvider: TokenSignKeyProvider,
    ): MetadataAuthorizationToken

    /**
     * Exchange a token for a new one with a different type or format.
     *
     * This is useful for exchanging refresh tokens to access tokens, or vice versa.
     *
     * @param token The token to exchange. For example, a refresh token to exchange for an access token.
     * If provides a raw token, will auto call [parseToken] to acquire parsed token.
     * @param tokenSignKeyProvider The signature provider to sign the new token.
     * @param newTokenType The type of the new token.
     * @param expiryDuration The expiry duration of the new token from now.
     * @param authorizedScopes The authorized scopes of the new token. If empty, the scopes of the original token will be used.
     * @param tokenFormat The format of the new token (Bearer, Basic, etc.).
     * @return The exchanged token.
     */
    @Throws(AuthenticationException::class)
    fun exchangeToken(
        token: AuthorizationToken,
        tokenSignKeyProvider: TokenSignKeyProvider,
        newTokenType: TokenType = TokenType.ACCESS,
        expiryDuration: Duration = Duration.ofHours(1),
        authorizedScopes: Collection<AuthorizationScope> = emptyList(),
        tokenFormat: TokenFormat = token.tokenFormat
    ): MetadataAuthorizationToken

    /**
     * Revoke a token, making it invalid for future use.
     *
     * If revoke a refresh token, the associated access token should also be revoked.
     */
    fun revokeToken(token: MetadataAuthorizationToken)

    /**
     * Check if a token is revoked.
     */
    fun isTokenRevoked(token: MetadataAuthorizationToken): Boolean
}