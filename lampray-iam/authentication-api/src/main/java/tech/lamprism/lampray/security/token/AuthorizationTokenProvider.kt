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

package tech.lamprism.lampray.security.token

import tech.lamprism.lampray.security.authorization.AuthorizationScope
import tech.lamprism.lampray.user.UserIdentity
import tech.lamprism.lampray.user.UserSignatureProvider
import java.time.Duration

/**
 * @author RollW
 */
interface AuthorizationTokenProvider {
    /**
     * Create a token for the user.
     *
     * @param user the user to create the token for.
     * @param signatureProvider the signature provider to sign the token.
     * @param expiryDuration the expiry duration of the token from now.
     * @param authorizedScopes the authorized scopes of the token. If empty,
     * use the default scopes of target user.
     * @return the token.
     */
    fun createToken(
        user: UserIdentity, // TODO: maybe replace with TokenSubject
        signatureProvider: UserSignatureProvider,
        expiryDuration: Duration = Duration.ofDays(1),
        authorizedScopes: Collection<AuthorizationScope> = emptyList()
    ): AuthorizationToken

    /**
     * Parse the token to get the user identity.
     *
     * @param token The token.
     * @param signatureProvider The signature provider to verify the token.
     * @return The user identity.
     * @throws InvalidTokenException If the token is invalid or expired.
     */
    @Throws(InvalidTokenException::class)
    fun parseToken(
        token: AuthorizationToken,
        signatureProvider: UserSignatureProvider
    ): UserIdentity // TODO: maybe replace with a MetadataToken

    fun supports(tokenType: String): Boolean
}