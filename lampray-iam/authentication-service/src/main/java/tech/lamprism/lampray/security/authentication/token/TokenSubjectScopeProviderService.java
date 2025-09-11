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

package tech.lamprism.lampray.security.authentication.token;

import org.springframework.stereotype.Service;
import tech.lamprism.lampray.security.authorization.AuthorizationScope;
import tech.lamprism.lampray.security.authorization.PrivilegedUser;
import tech.lamprism.lampray.security.authorization.PrivilegedUserProvider;
import tech.lamprism.lampray.security.token.SubjectType;
import tech.lamprism.lampray.security.token.TokenSubject;
import tech.lamprism.lampray.security.token.TokenSubjectScopeProvider;

import java.util.Collection;

/**
 * @author RollW
 */
@Service
public class TokenSubjectScopeProviderService implements TokenSubjectScopeProvider {
    private final PrivilegedUserProvider privilegedUserProvider;

    public TokenSubjectScopeProviderService(PrivilegedUserProvider privilegedUserProvider) {
        this.privilegedUserProvider = privilegedUserProvider;
    }

    @Override
    public Collection<AuthorizationScope> getAuthorizationScopes(TokenSubject tokenSubject) {
        if (tokenSubject.getType() != SubjectType.USER) {
            throw new IllegalArgumentException("Subject type not supported: " + tokenSubject.getType());
        }

        PrivilegedUser privilegedUser = privilegedUserProvider.loadPrivilegedUserById(Long.parseLong(tokenSubject.getId()));
        return privilegedUser.getScopes();
    }
}
