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

/**
 * A raw authorization token with no additional metadata.
 *
 * @author RollW
 */
interface AuthorizationToken {
    /**
     * The transport format of the token (e.g., "Bearer", "Basic")
     */
    val tokenFormat: TokenFormat

    /**
     * The functional type of the token.
     *
     * - For a raw token, this means the *expected* type of the token. And the parser will try to verify
     *   the token type matches this expected type.
     * - For a parsed token, this is the actual type of the token.
     */
    val tokenType: TokenType

    /**
     * The actual token string
     */
    val token: String

    /**
     * If the token is authorized, it means the token is valid and can be used to access resources.
     * This is always false for a raw token, as it has not been parsed or verified.
     */
    val authorized: Boolean
        get() = false
}
