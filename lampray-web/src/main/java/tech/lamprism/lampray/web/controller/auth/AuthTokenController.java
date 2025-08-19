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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.lamprism.lampray.security.token.AuthorizationTokenManager;
import tech.lamprism.lampray.security.token.AuthorizationTokenUtils;
import tech.lamprism.lampray.security.token.BearerAuthorizationToken;
import tech.lamprism.lampray.security.token.MetadataAuthorizationToken;
import tech.lamprism.lampray.security.token.TokenFormat;
import tech.lamprism.lampray.security.token.TokenSignKeyProvider;
import tech.lamprism.lampray.security.token.TokenType;
import tech.lamprism.lampray.web.controller.auth.model.RefreshTokenRequest;
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
    private final TokenSignKeyProvider tokenSignKeyProvider;

    public AuthTokenController(AuthorizationTokenManager authorizationTokenManager,
                               TokenSignKeyProvider tokenSignKeyProvider) {
        this.authorizationTokenManager = authorizationTokenManager;
        this.tokenSignKeyProvider = tokenSignKeyProvider;
    }

    // TODO: support refresh token
    @PostMapping("/token:refresh")
    public HttpResponseEntity<String> refreshToken(
            @RequestBody RefreshTokenRequest refreshTokenRequest) {
        MetadataAuthorizationToken exchangedToken = authorizationTokenManager.exchangeToken(
                new BearerAuthorizationToken(refreshTokenRequest.getRefreshToken(), TokenType.REFRESH),
                tokenSignKeyProvider, TokenType.ACCESS, Duration.ofHours(1),
                List.of(), TokenFormat.BEARER
        );
        return HttpResponseEntity.success(
                AuthorizationTokenUtils.toHeaderValue(exchangedToken)
        );
    }

    @GetMapping("/token:verify")
    public HttpResponseEntity<Void> verifyToken(
            @RequestParam String token) {
//        MetadataAuthorizationToken metadataAuthorizationToken = authorizationTokenProvider.parseToken(
//                new BearerAuthorizationToken(token),
//                userSignatureProvider
//        );
        return HttpResponseEntity.success();
    }
}
