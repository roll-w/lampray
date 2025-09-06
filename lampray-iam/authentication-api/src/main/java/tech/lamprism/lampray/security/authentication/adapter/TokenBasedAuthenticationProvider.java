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

package tech.lamprism.lampray.security.authentication.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import tech.lamprism.lampray.security.authorization.AuthorizationScope;
import tech.lamprism.lampray.security.authorization.PrivilegedUser;
import tech.lamprism.lampray.security.authorization.PrivilegedUserProvider;
import tech.lamprism.lampray.security.authorization.adapter.PrivilegedUserAuthenticationToken;
import tech.lamprism.lampray.security.authorization.hierarchy.AuthorizationScopeHierarchy;
import tech.lamprism.lampray.security.token.AuthorizationToken;
import tech.lamprism.lampray.security.token.AuthorizationTokenManager;
import tech.lamprism.lampray.security.token.MetadataAuthorizationToken;
import tech.lamprism.lampray.security.token.SimpleAuthorizationToken;
import tech.lamprism.lampray.security.token.TokenFormat;
import tech.lamprism.lampray.security.token.TokenSubject;
import tech.lamprism.lampray.security.token.TokenSubjectSignKeyProvider;
import tech.lamprism.lampray.security.token.TokenType;
import tech.rollw.common.web.CommonRuntimeException;

import java.util.Collection;

/**
 * @author RollW
 */
public class TokenBasedAuthenticationProvider extends PrivilegedUserBasedAuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(TokenBasedAuthenticationProvider.class);

    private final AuthorizationTokenManager authorizationTokenManager;
    private final PrivilegedUserProvider privilegedUserProvider;
    private final TokenSubjectSignKeyProvider tokenSubjectSignKeyProvider;
    private final AuthorizationScopeHierarchy authorizationScopeHierarchy;

    public TokenBasedAuthenticationProvider(
            AuthorizationTokenManager authorizationTokenManager,
            PrivilegedUserProvider privilegedUserProvider,
            TokenSubjectSignKeyProvider tokenSubjectSignKeyProvider,
            AuthorizationScopeHierarchy authorizationScopeHierarchy
    ) {
        this.authorizationTokenManager = authorizationTokenManager;
        this.privilegedUserProvider = privilegedUserProvider;
        this.tokenSubjectSignKeyProvider = tokenSubjectSignKeyProvider;
        this.authorizationScopeHierarchy = authorizationScopeHierarchy;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        TokenBasedAuthenticationToken tokenBasedAuthenticationToken = (TokenBasedAuthenticationToken) authentication;
        AuthorizationToken token = parseCredentials(tokenBasedAuthenticationToken.getCredentials());
        try {
            MetadataAuthorizationToken authorizationToken = authorizationTokenManager.parseToken(
                    token, tokenSubjectSignKeyProvider);
            TokenSubject subject = authorizationToken.getSubject();
            Collection<AuthorizationScope> reachableAuthorizationScopes = authorizationScopeHierarchy
                    .getReachableAuthorizationScopes(authorizationToken.getScopes());
            return switch (subject.getType()) {
                case USER -> {
                    PrivilegedUser privilegedUser = privilegedUserProvider.loadPrivilegedUserById(
                            Long.parseLong(subject.getId())
                    );
                    check(privilegedUser);
                    yield new PrivilegedUserAuthenticationToken(privilegedUser, reachableAuthorizationScopes);
                }
                // TODO: other subject type was not yet supported
                default -> throw new BadCredentialsException("Not support subject type.");
            };
        } catch (CommonRuntimeException e) {
            throw new TokenAuthenticationException(e);
        }
    }

    private AuthorizationToken parseCredentials(String credentials) {
        // Parse header, format like: "<Type> <Token>"
        int index = credentials.indexOf(' ');
        if (index < 0) {
            throw new BadCredentialsException("Invalid token format");
        }
        String type = credentials.substring(0, index);
        String token = credentials.substring(index + 1);
        TokenFormat tokenFormat = TokenFormat.fromValue(type);
        if (tokenFormat == null) {
            throw new BadCredentialsException("Invalid token format");
        }
        return new SimpleAuthorizationToken(token, TokenType.ACCESS);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.isAssignableFrom(TokenBasedAuthenticationToken.class);
    }
}
