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

package tech.lamprism.lampray.security.authentication.token.jwt

import tech.lamprism.lampray.security.authorization.AuthorizationScope
import tech.lamprism.lampray.security.token.AuthorizationToken
import tech.lamprism.lampray.security.token.RefreshMetadataAuthorizationToken
import tech.lamprism.lampray.security.token.TokenFormat
import tech.lamprism.lampray.security.token.TokenSubject
import tech.lamprism.lampray.security.token.TokenType
import java.time.OffsetDateTime

/**
 * @author RollW
 */
data class SimpleRefreshMetadataAuthorizationToken(
    override val token: String,
    override val tokenType: TokenType,
    override val subject: TokenSubject,
    override val tokenId: String,
    override val scopes: List<AuthorizationScope>,
    override val permittedScopes: List<AuthorizationScope>,
    override val expirationAt: OffsetDateTime,
    override val tokenFormat: TokenFormat
) : RefreshMetadataAuthorizationToken {

    constructor(
        authorizationToken: AuthorizationToken,
        subject: TokenSubject,
        tokenId: String,
        scopes: List<AuthorizationScope>,
        permittedScopes: List<AuthorizationScope>,
        expirationTime: OffsetDateTime,
    ) : this(
        token = authorizationToken.token,
        tokenType = authorizationToken.tokenType,
        tokenFormat = authorizationToken.tokenFormat,
        tokenId = tokenId,
        expirationAt = expirationTime,
        subject = subject,
        scopes = scopes,
        permittedScopes = permittedScopes
    )
}