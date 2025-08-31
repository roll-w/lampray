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

package tech.lamprism.lampray.web.controller.auth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.lamprism.lampray.security.token.AuthorizationTokenConfigKeys;
import tech.lamprism.lampray.security.token.AuthorizationTokenManager;
import tech.lamprism.lampray.security.token.MetadataAuthorizationToken;
import tech.lamprism.lampray.security.token.SimpleAuthorizationToken;
import tech.lamprism.lampray.security.token.TokenSubjectSignKeyProvider;
import tech.lamprism.lampray.security.token.TokenType;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.controller.auth.model.RefreshTokenRequest;
import tech.lamprism.lampray.web.controller.auth.model.RefreshTokenResponse;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;
import tech.rollw.common.web.HttpResponseEntity;

import java.time.Duration;
import java.util.List;

/**
 * @author RollW
 */
@RestController
@RequestMapping("/api/v1/auth/")
public class AuthTokenController {
    private final AuthorizationTokenManager authorizationTokenManager;
    private final TokenSubjectSignKeyProvider tokenSubjectSignKeyProvider;
    private final ConfigReader configReader;

    public AuthTokenController(AuthorizationTokenManager authorizationTokenManager,
                               TokenSubjectSignKeyProvider tokenSubjectSignKeyProvider,
                               ConfigReader configReader) {
        this.authorizationTokenManager = authorizationTokenManager;
        this.tokenSubjectSignKeyProvider = tokenSubjectSignKeyProvider;
        this.configReader = configReader;
    }

    @PostMapping("/token:refresh")
    public HttpResponseEntity<RefreshTokenResponse> refreshToken(
            @RequestBody RefreshTokenRequest refreshTokenRequest) {
        if (refreshTokenRequest == null || StringUtils.isBlank(refreshTokenRequest.getRefreshToken())) {
            throw new CommonRuntimeException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Refresh token is required");
        }

        Long accessTokenExpireTime = configReader.get(AuthorizationTokenConfigKeys.ACCESS_TOKEN_EXPIRE_TIME);
        MetadataAuthorizationToken exchangedToken = authorizationTokenManager.exchangeToken(
                new SimpleAuthorizationToken(refreshTokenRequest.getRefreshToken(), TokenType.REFRESH),
                tokenSubjectSignKeyProvider,
                TokenType.ACCESS,
                Duration.ofSeconds(accessTokenExpireTime),
                List.of()
        );
        return HttpResponseEntity.success(
                new RefreshTokenResponse(exchangedToken.getToken(), exchangedToken.getExpirationAt())
        );
    }

    @GetMapping("/token:verify")
    public HttpResponseEntity<Void> verifyToken() {
        return HttpResponseEntity.success();
    }
}
