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

package tech.lamprism.lampray.authentication.token;

import tech.rollw.common.web.system.AuthenticationException;

/**
 * Authentication token service.
 *
 * @deprecated Move to {@link tech.lamprism.lampray.security.token.AuthorizationTokenProvider}
 * @author RollW
 */
@Deprecated
public interface AuthenticationTokenService {
    String generateAuthToken(long userId, String signature);

    String generateAuthToken(long userId, String signature,
                             long expireTimeInSecond);

    /**
     * Only verifies the legality of the token
     * does not take responsibility for confirming user status.
     */
    TokenAuthResult verifyToken(String token, String signature)
            throws AuthenticationException;

    Long getUserId(String token);
}
