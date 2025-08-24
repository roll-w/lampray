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

package tech.lamprism.lampray.security.authentication.token.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import space.lingu.NonNull;
import tech.lamprism.lampray.security.authorization.AuthorizationScope;
import tech.lamprism.lampray.security.authorization.AuthorizationScopeProvider;
import tech.lamprism.lampray.security.token.AuthorizationToken;
import tech.lamprism.lampray.security.token.MetadataAuthorizationToken;
import tech.lamprism.lampray.security.token.RefreshTokenAuthorizationScope;
import tech.lamprism.lampray.security.token.TokenSubject;
import tech.lamprism.lampray.security.token.TokenSubjectProvider;
import tech.lamprism.lampray.security.token.TokenSubjectSignKeyProvider;
import tech.lamprism.lampray.security.token.TokenType;
import tech.lamprism.lampray.setting.ConfigReader;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/**
 * @author RollW
 */
@Service
public class RefreshJwtAuthorizationTokenProvider extends AbstractJwtAuthorizationTokenProvider {
    public static final String PERMITTED_SCOPES_FIELD = "permitted_scopes";

    public RefreshJwtAuthorizationTokenProvider(ConfigReader configReader, AuthorizationScopeProvider authorizationScopeProvider,
                                                TokenSubjectProvider tokenSubjectProvider) {
        super(configReader, authorizationScopeProvider, tokenSubjectProvider);
    }

    @Override
    protected void buildJwt(@NonNull TokenSubject subject,
                            @NonNull TokenSubjectSignKeyProvider tokenSubjectSignKeyProvider,
                            @NonNull String tokenId,
                            @NonNull TokenType tokenType,
                            @NonNull Duration expiryDuration,
                            @NonNull Collection<? extends AuthorizationScope> authorizedScopes,
                            @NonNull JwtBuilder builder) {
        builder.claim(SCOPES_FIELD, List.of(RefreshTokenAuthorizationScope.INSTANCE.getScope()));
        builder.claim(TOKEN_ID_FIELD, tokenId);
        builder.claim(PERMITTED_SCOPES_FIELD, authorizedScopes.stream()
                .map(AuthorizationScope::getScope)
                .toList());
    }

    @Override
    protected MetadataAuthorizationToken constructMetadataAuthorizationToken(
            @NonNull AuthorizationToken token,
            @NonNull TokenType tokenType,
            @NonNull TokenSubject subject,
            @NonNull String tokenId,
            @NonNull List<? extends AuthorizationScope> authorizedScopes,
            @NonNull OffsetDateTime expirationTime,
            @NonNull Jws<Claims> jws) {
        Claims payload = jws.getPayload();
        List<String> permittedScopes = payload.get(PERMITTED_SCOPES_FIELD, List.class);
        List<AuthorizationScope> permittedAuthorizationScopes = authorizationScopeProvider.findScopes(permittedScopes);
        return new SimpleRefreshMetadataAuthorizationToken(
                token, subject, tokenId, authorizedScopes,
                permittedAuthorizationScopes,
                expirationTime
        );
    }

    @Override
    public boolean supports(@NotNull TokenType tokenType) {
        return tokenType == TokenType.REFRESH;
    }
}
