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

/**
 * Represents a refresh token for metadata authorization.
 *
 * Refresh token cannot be used to access resources that are not explicitly allowed (except for the refresh operation itself).
 *
 * @author RollW
 */
interface RefreshMetadataAuthorizationToken : MetadataAuthorizationToken {

    /**
     * The scopes that are permitted for this token, when use the refresh token to
     * exchange for a new access token, the new access token will have these scopes
     * granted by default.
     */
    val permittedScopes: List<AuthorizationScope>
}