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

package tech.lamprism.lampray.security.token;

import java.time.OffsetDateTime;

/**
 * Interface for storage of revoked tokens.
 * <p>
 * Token revocation is a part of the security mechanism to block leaked or compromised tokens.
 *
 * @author RollW
 */
public interface RevokeTokenStorage {
    void revokeToken(MetadataAuthorizationToken token);

    boolean isTokenRevoked(MetadataAuthorizationToken token);

    void cleanupExpiredRevocations(OffsetDateTime expiredAt);
}
