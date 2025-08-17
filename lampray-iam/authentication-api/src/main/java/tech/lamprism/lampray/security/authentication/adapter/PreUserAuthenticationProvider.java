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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import tech.lamprism.lampray.security.authorization.PrivilegedUser;
import tech.lamprism.lampray.security.authorization.PrivilegedUserProvider;
import tech.lamprism.lampray.security.authorization.adapter.PrivilegedUserAuthenticationToken;
import tech.rollw.common.web.CommonRuntimeException;

/**
 * @author RollW
 */
public class PreUserAuthenticationProvider extends PrivilegedUserBasedAuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(PreUserAuthenticationProvider.class);

    private final PrivilegedUserProvider privilegedUserProvider;

    public PreUserAuthenticationProvider(PrivilegedUserProvider privilegedUserProvider) {
        this.privilegedUserProvider = privilegedUserProvider;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof PreUserAuthenticationToken token)) {
            throw new IllegalArgumentException("Authentication must be an instance of PreUserAuthenticationToken");
        }
        try {
            PrivilegedUser privilegedUser = privilegedUserProvider.loadPrivilegedUserById(
                    token.getCredentials().getUserId()
            );
            check(privilegedUser);
            return new PrivilegedUserAuthenticationToken(privilegedUser);
        } catch (CommonRuntimeException e) {
            throw new TokenAuthenticationException(e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.isAssignableFrom(PreUserAuthenticationToken.class);
    }
}
