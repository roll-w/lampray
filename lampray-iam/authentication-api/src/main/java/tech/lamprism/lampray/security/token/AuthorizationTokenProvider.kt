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
 * Enhanced authorization token provider that supports different token types and formats.
 *
 * @author RollW
 */
interface AuthorizationTokenProvider {
    /**
     * Create a token for the user with specified type.
     *
     * @param subject the subject to create the token for.
     * @param tokenSignKeyProvider the signature provider to sign the token.
     * @param tokenType the type of token to create.
     * @param expiryDuration the expiry duration of the token from now.
     * @param authorizedScopes the authorized scopes of the token.
     * @param tokenFormat the format of the token (Bearer, Basic, etc.).
     * @return the token.
     */
    fun createToken(
        subject: TokenSubject,
        tokenSignKeyProvider: TokenSignKeyProvider,
        tokenId: String,
        tokenType: TokenType,
        expiryDuration: Duration = Duration.ofDays(1),
        authorizedScopes: Collection<AuthorizationScope> = emptyList(),
        tokenFormat: TokenFormat = TokenFormat.BEARER
    ): MetadataAuthorizationToken

    /**
     * Parse the token to get the user identity.
     *
     * @param token The token.
     * @param tokenSignKeyProvider The signature provider to verify the token.
     * @return The user identity.
     * @throws AuthenticationException If the token is invalid or expired.
     */
    @Throws(AuthenticationException::class)
    fun parseToken(
        // TODO: support parse unknown token type
        token: AuthorizationToken,
        tokenSignKeyProvider: TokenSignKeyProvider
    ): MetadataAuthorizationToken

    fun supports(tokenType: TokenType): Boolean
}