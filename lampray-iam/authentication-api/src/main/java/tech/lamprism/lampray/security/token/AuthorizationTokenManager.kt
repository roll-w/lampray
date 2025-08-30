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
 * Manages the creation, parsing, exchange, and revocation of authorization tokens.
 *
 * @author RollW
 */
interface AuthorizationTokenManager {
    /**
     * Create a token with specified type and format.
     *
     * @param subject The subject of the token, which can be a user or a service.
     * @param tokenSubjectSignKeyProvider The signature provider to sign the token.
     * @param tokenType The type of the token (e.g., ACCESS, REFRESH).
     * @param expiryDuration The expiry duration of the token from now.
     * @param authorizedScopes The authorized scopes of the token. If empty, no scopes are authorized.
     * @return The created token with metadata.
     */
    fun createToken(
        subject: TokenSubject,
        tokenSubjectSignKeyProvider: TokenSubjectSignKeyProvider,
        tokenType: TokenType = TokenType.ACCESS,
        expiryDuration: Duration = Duration.ofHours(1),
        authorizedScopes: Collection<AuthorizationScope> = emptyList()
    ): MetadataAuthorizationToken

    /**
     * Parse the token to get the user identity and metadata.
     *
     * @param token The token.
     * @param tokenSubjectSignKeyProvider The signature provider to verify the token.
     * @return The parsed token with metadata.
     * @throws AuthenticationException If the token is invalid or expired.
     */
    @Throws(AuthenticationException::class)
    fun parseToken(
        token: AuthorizationToken,
        tokenSubjectSignKeyProvider: TokenSubjectSignKeyProvider,
    ): MetadataAuthorizationToken

    /**
     * Exchange a token for a new one with a different type or format.
     *
     * This is useful for exchanging refresh tokens to access tokens, or vice versa.
     *
     * @param token The token to exchange. For example, a refresh token to exchange for an access token.
     * If provides a raw token, will auto call [parseToken] to acquire parsed token.
     * @param tokenSubjectSignKeyProvider The signature provider to sign the new token.
     * @param newTokenType The type of the new token.
     * @param expiryDuration The expiry duration of the new token from now.
     * @param authorizedScopes The authorized scopes of the new token, can be the same or a subset of the original token's scopes.
     * If empty, will use the permission scopes of the original token (i.e., for a refresh token, will use the permitted scopes of it).
     * @return The exchanged token.
     */
    @Throws(AuthenticationException::class)
    fun exchangeToken(
        token: AuthorizationToken,
        tokenSubjectSignKeyProvider: TokenSubjectSignKeyProvider,
        newTokenType: TokenType = TokenType.ACCESS,
        expiryDuration: Duration = Duration.ofHours(1),
        authorizedScopes: Collection<AuthorizationScope> = emptyList()
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