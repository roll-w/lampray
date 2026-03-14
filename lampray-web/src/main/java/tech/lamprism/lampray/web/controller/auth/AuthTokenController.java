/*
 * Copyright (C) 2023-2026 RollW
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.lamprism.lampray.security.token.AuthorizationTokenConfigKeys;
import tech.lamprism.lampray.security.token.AuthorizationTokenManager;
import tech.lamprism.lampray.security.token.MetadataAuthorizationToken;
import tech.lamprism.lampray.security.token.SimpleAuthorizationToken;
import tech.lamprism.lampray.security.token.TokenSubjectSignKeyProvider;
import tech.lamprism.lampray.security.token.TokenType;
import tech.lamprism.lampray.setting.ConfigReader;
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
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = RefreshTokenCookieHelper.resolveRefreshToken(request);
        if (StringUtils.isBlank(refreshToken)) {
            RefreshTokenCookieHelper.clearRefreshTokenCookie(request, response);
            throw new CommonRuntimeException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Refresh token is required");
        }

        Long accessTokenExpireTime = configReader.get(AuthorizationTokenConfigKeys.ACCESS_TOKEN_EXPIRE_TIME);
        MetadataAuthorizationToken exchangedToken;
        try {
            exchangedToken = authorizationTokenManager.exchangeToken(
                    new SimpleAuthorizationToken(refreshToken, TokenType.REFRESH),
                    tokenSubjectSignKeyProvider,
                    TokenType.ACCESS,
                    Duration.ofSeconds(accessTokenExpireTime),
                    List.of()
            );
        } catch (RuntimeException e) {
            RefreshTokenCookieHelper.clearRefreshTokenCookie(request, response);
            throw e;
        }
        return HttpResponseEntity.success(
                new RefreshTokenResponse(exchangedToken.getToken(), exchangedToken.getExpirationAt())
        );
    }

    @GetMapping("/token:verify")
    public HttpResponseEntity<Void> verifyToken() {
        return HttpResponseEntity.success();
    }
}
