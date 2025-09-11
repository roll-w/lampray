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

package tech.lamprism.lampray.web.controller.user.model;

import tech.lamprism.lampray.security.token.MetadataAuthorizationToken;
import tech.lamprism.lampray.user.UserIdentity;

import java.time.OffsetDateTime;

/**
 * @author RollW
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiry,
        OffsetDateTime refreshTokenExpiry,
        UserVo user
) {
    public LoginResponse(MetadataAuthorizationToken accessToken,
                         MetadataAuthorizationToken refreshToken,
                         UserIdentity userIdentity) {
        this(accessToken.getToken(), refreshToken.getToken(),
                accessToken.getExpirationAt(), refreshToken.getExpirationAt(),
                UserVo.toVo(userIdentity));
    }

    public static final LoginResponse NULL = new LoginResponse(
            null, null, null, null, null);

    public static LoginResponse nullResponse() {
        return NULL;
    }
}
