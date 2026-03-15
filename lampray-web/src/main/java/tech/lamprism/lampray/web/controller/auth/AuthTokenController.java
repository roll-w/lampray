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
import tech.lamprism.lampray.security.token.RefreshMetadataAuthorizationToken;
import tech.lamprism.lampray.security.token.SimpleAuthorizationToken;
import tech.lamprism.lampray.security.token.TokenSubjectSignKeyProvider;
import tech.lamprism.lampray.security.token.TokenType;
import tech.lamprism.lampray.setting.ConfigReader;
import tech.lamprism.lampray.web.controller.auth.model.RefreshTokenResponse;
import tech.rollw.common.web.CommonErrorCode;
import tech.rollw.common.web.CommonRuntimeException;
import tech.rollw.common.web.HttpResponseEntity;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

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
        RefreshRequestContext context = resolveRefreshRequestContext(request, response);

        try {
            RefreshMetadataAuthorizationToken parsedRefreshToken = parseRefreshToken(context.rawRefreshToken());
            MetadataAuthorizationToken activeRefreshToken = resolveActiveRefreshToken(
                    parsedRefreshToken,
                    request,
                    response,
                    context
            );
            MetadataAuthorizationToken accessToken = exchangeAccessToken(
                    activeRefreshToken,
                    context.accessTokenExpiry()
            );
            return HttpResponseEntity.success(toRefreshTokenResponse(accessToken, activeRefreshToken));
        } catch (RuntimeException e) {
            RefreshTokenCookieHelper.clearRefreshTokenCookie(request, response);
            throw e;
        }
    }

    private RefreshRequestContext resolveRefreshRequestContext(HttpServletRequest request,
                                                               HttpServletResponse response) {
        String rawRefreshToken = RefreshTokenCookieHelper.resolveRefreshToken(request);
        if (StringUtils.isBlank(rawRefreshToken)) {
            RefreshTokenCookieHelper.clearRefreshTokenCookie(request, response);
            throw new CommonRuntimeException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Refresh token is required");
        }

        Duration accessTokenExpiry = Duration.ofSeconds(Objects.requireNonNull(
                configReader.get(AuthorizationTokenConfigKeys.ACCESS_TOKEN_EXPIRE_TIME)
        ));
        Duration refreshTokenExpiry = Duration.ofSeconds(Objects.requireNonNull(
                configReader.get(AuthorizationTokenConfigKeys.REFRESH_TOKEN_EXPIRE_TIME)
        ));

        return new RefreshRequestContext(
                rawRefreshToken,
                accessTokenExpiry,
                refreshTokenExpiry,
                RefreshTokenCookieHelper.resolveRememberMe(request)
        );
    }

    private MetadataAuthorizationToken resolveActiveRefreshToken(
            RefreshMetadataAuthorizationToken parsedRefreshToken,
            HttpServletRequest request,
            HttpServletResponse response,
            RefreshRequestContext context) {
        if (!shouldRotateRefreshToken(parsedRefreshToken.getExpirationAt(), context.accessTokenExpiry())) {
            return parsedRefreshToken;
        }

        MetadataAuthorizationToken rotatedRefreshToken = authorizationTokenManager.createToken(
                parsedRefreshToken.getSubject(),
                tokenSubjectSignKeyProvider,
                TokenType.REFRESH,
                context.refreshTokenExpiry(),
                parsedRefreshToken.getPermittedScopes()
        );
        authorizationTokenManager.revokeToken(parsedRefreshToken);
        RefreshTokenCookieHelper.writeRefreshTokenCookie(
                request,
                response,
                rotatedRefreshToken.getToken(),
                context.rememberMe(),
                context.refreshTokenExpiry()
        );
        return rotatedRefreshToken;
    }

    private MetadataAuthorizationToken exchangeAccessToken(MetadataAuthorizationToken refreshToken,
                                                           Duration accessTokenExpiry) {
        return authorizationTokenManager.exchangeToken(
                refreshToken,
                tokenSubjectSignKeyProvider,
                TokenType.ACCESS,
                accessTokenExpiry,
                List.of()
        );
    }

    private RefreshTokenResponse toRefreshTokenResponse(MetadataAuthorizationToken accessToken,
                                                        MetadataAuthorizationToken refreshToken) {
        return new RefreshTokenResponse(
                accessToken.getToken(),
                accessToken.getExpirationAt(),
                refreshToken.getExpirationAt()
        );
    }

    private RefreshMetadataAuthorizationToken parseRefreshToken(String refreshToken) {
        MetadataAuthorizationToken parsedToken = authorizationTokenManager.parseToken(
                new SimpleAuthorizationToken(refreshToken, TokenType.REFRESH),
                tokenSubjectSignKeyProvider
        );
        if (parsedToken instanceof RefreshMetadataAuthorizationToken refreshMetadataAuthorizationToken) {
            return refreshMetadataAuthorizationToken;
        }

        throw new CommonRuntimeException(CommonErrorCode.ERROR_ILLEGAL_ARGUMENT, "Refresh token is invalid");
    }

    private boolean shouldRotateRefreshToken(OffsetDateTime refreshTokenExpiryAt,
                                             Duration accessTokenExpiry) {
        return !refreshTokenExpiryAt.isAfter(OffsetDateTime.now().plus(accessTokenExpiry));
    }

    private record RefreshRequestContext(String rawRefreshToken,
                                         Duration accessTokenExpiry,
                                         Duration refreshTokenExpiry,
                                         boolean rememberMe) {
    }

    @GetMapping("/token:verify")
    public HttpResponseEntity<Void> verifyToken() {
        return HttpResponseEntity.success();
    }
}
