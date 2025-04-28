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
import tech.lamprism.lampray.security.authorization.Privilege
import tech.lamprism.lampray.user.UserIdentity
import java.time.OffsetDateTime

typealias TokenSubject = UserIdentity

/**
 * Token for authorization with metadata. Used for a parsed-down version of [AuthorizationToken].
 *
 * @author RollW
 */
interface MetadataAuthorizationToken : AuthorizationToken, Privilege {
    val expirationAt: OffsetDateTime

    // TODO: subject maybe change to a data class or generic type
    val subject : TokenSubject

    override val scopes: List<AuthorizationScope>
}