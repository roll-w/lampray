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
import tech.lamprism.lampray.security.authorization.PrivilegedUser;
import tech.lamprism.lampray.security.authorization.PrivilegedUserProvider;
import tech.lamprism.lampray.security.authorization.adapter.PrivilegedUserAuthenticationToken;
import tech.lamprism.lampray.security.token.AuthorizationToken;
import tech.lamprism.lampray.security.token.AuthorizationTokenProvider;
import tech.lamprism.lampray.security.token.MetadataAuthorizationToken;
import tech.lamprism.lampray.security.token.SimpleAuthorizationToken;
import tech.lamprism.lampray.user.UserSignatureProvider;
import tech.rollw.common.web.CommonRuntimeException;

/**
 * @author RollW
 */
public class TokenBasedAuthenticationProvider extends PrivilegedUserBasedAuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(TokenBasedAuthenticationProvider.class);

    private final AuthorizationTokenProvider authorizationTokenProvider;
    private final PrivilegedUserProvider privilegedUserProvider;
    private final UserSignatureProvider userSignatureProvider;

    public TokenBasedAuthenticationProvider(
            AuthorizationTokenProvider authorizationTokenProvider,
            PrivilegedUserProvider privilegedUserProvider,
            UserSignatureProvider userSignatureProvider
    ) {
        this.authorizationTokenProvider = authorizationTokenProvider;
        this.privilegedUserProvider = privilegedUserProvider;
        this.userSignatureProvider = userSignatureProvider;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        TokenBasedAuthenticationToken tokenBasedAuthenticationToken = (TokenBasedAuthenticationToken) authentication;
        AuthorizationToken token = parseToken(tokenBasedAuthenticationToken.getCredentials());
        try {
            MetadataAuthorizationToken authorizationToken = authorizationTokenProvider.parseToken(
                    token, userSignatureProvider);
            PrivilegedUser privilegedUser = privilegedUserProvider.loadPrivilegedUser(
                    authorizationToken.getSubject());
            check(privilegedUser);
            return new PrivilegedUserAuthenticationToken(privilegedUser);
        } catch (CommonRuntimeException e) {
            throw new TokenAuthenticationException(e);
        }
    }

    private AuthorizationToken parseToken(String credentials) {
        // Parse header, format like: "<Type> <Token>"
        int index = credentials.indexOf(' ');
        if (index < 0) {
            throw new BadCredentialsException("Invalid token format");
        }
        String type = credentials.substring(0, index);
        String token = credentials.substring(index + 1);
        return new SimpleAuthorizationToken(type, token);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.isAssignableFrom(TokenBasedAuthenticationToken.class);
    }
}
